const https = require('https');
const fs = require('fs');
const path = require('path');
const zlib = require('zlib');
const net = require('net'); // Re-used for utilities if needed

// Simplified TAR unpacker (Paxos/Ustar format handling for typical NPM tarballs)
// Note: This is a rough implementation to avoid external dependencies like 'tar' or 'adm-zip'
function unpackTarGz(buffer, destDir) {
    try {
        const unzipped = zlib.gunzipSync(buffer);
        console.log("Unzipped size: " + unzipped.length);
        
        let offset = 0;
        while (offset < unzipped.length - 512) {
            // Read header
            const name = unzipped.toString('utf8', offset, offset + 100).replace(/\0/g, '').trim();
            if (!name) break;
            
            const sizeStr = unzipped.toString('utf8', offset + 124, offset + 136).replace(/\0/g, '').trim();
            const size = parseInt(sizeStr, 8);
            const type = unzipped.toString('utf8', offset + 156, offset + 157);
            
            offset += 512; // Skip header
            
            // Normalize path (npm tarballs usually start with 'package/')
            let safePath = name;
            if (safePath.startsWith('package/')) safePath = safePath.substring(8);
            
            const fullPath = path.join(destDir, safePath);
            
            // Log for debug
            // console.log(`Extracting: ${safePath} (${size} bytes)`);

            if (type === '5') { // Directory
                 if (!fs.existsSync(fullPath)) fs.mkdirSync(fullPath, { recursive: true });
            } else if (type === '0' || type === '' || type === '7') { // File
                 // Ensure parent dir exists
                 const parent = path.dirname(fullPath);
                 if (!fs.existsSync(parent)) fs.mkdirSync(parent, { recursive: true });
                 
                 const fileData = unzipped.subarray(offset, offset + size);
                 fs.writeFileSync(fullPath, fileData);
            }
            
            // Align to 512 byte block
            offset += Math.ceil(size / 512) * 512;
        }
        return true;
    } catch (e) {
        console.error("Unpack Error: " + e.message);
        return false;
    }
}

async function install(pkgName, version = 'latest') {
    console.log(`Installing ${pkgName}@${version}...`);
    
    // 1. Get Metadata
    const meta = await fetchJson(`https://registry.npmjs.org/${pkgName}/${version}`);
    if (!meta || !meta.dist || !meta.dist.tarball) {
        throw new Error(`Package ${pkgName} not found or invalid metadata.`);
    }
    
    // 2. Download Tarball
    const tarballUrl = meta.dist.tarball;
    console.log(`Downloading ${tarballUrl}...`);
    const tgz = await fetchBuffer(tarballUrl);
    
    // 3. Prepare dirs
    const modulesDir = path.join(process.cwd(), 'node_modules');
    if (!fs.existsSync(modulesDir)) fs.mkdirSync(modulesDir);
    
    const targetDir = path.join(modulesDir, pkgName);
    // Cleanup old
    if (fs.existsSync(targetDir)) {
        // Simple recursive delete
        fs.rmSync(targetDir, { recursive: true, force: true });
    }
    fs.mkdirSync(targetDir, { recursive: true });
    
    // 4. Extract
    console.log(`Extracting to ${targetDir}...`);
    const success = unpackTarGz(tgz, targetDir);
    if (!success) throw new Error("Failed to extract package.");
    
    // 5. Handle Dependencies (Simple Depth-1)
    if (meta.dependencies) {
        console.log("Installing dependencies: " + Object.keys(meta.dependencies).join(', '));
        for (const dep of Object.keys(meta.dependencies)) {
            try {
                // Determine version (simplified, strip ^/~)
                let ver = meta.dependencies[dep].replace(/[\^~]/g, '');
                if (ver === '*') ver = 'latest';
                // Check if already exists to avoid infinite loops in this naive installer
                if (!fs.existsSync(path.join(modulesDir, dep))) {
                    await install(dep, ver);
                }
            } catch (e) {
                console.error(`Failed to install dependency ${dep}: ${e.message}`);
            }
        }
    }
    
    console.log(`Successfully installed ${pkgName}`);
    return true;
}

function fetchJson(url) {
    return new Promise((resolve, reject) => {
        https.get(url, (res) => {
            if (res.statusCode !== 200) {
                res.resume();
                return reject(new Error(`Request failed: ${res.statusCode}`));
            }
            let data = '';
            res.on('data', c => data += c);
            res.on('end', () => {
                try { resolve(JSON.parse(data)); } catch(e) { reject(e); }
            });
        }).on('error', reject);
    });
}

function fetchBuffer(url) {
    return new Promise((resolve, reject) => {
        https.get(url, (res) => {
            if (res.statusCode !== 200) {
                res.resume();
                return reject(new Error(`Request failed: ${res.statusCode}`));
            }
            const data = [];
            res.on('data', c => data.push(c));
            res.on('end', () => resolve(Buffer.concat(data)));
        }).on('error', reject);
    });
}

function list() {
    const modulesDir = path.join(process.cwd(), 'node_modules');
    if (!fs.existsSync(modulesDir)) return [];
    return fs.readdirSync(modulesDir).filter(n => !n.startsWith('.'));
}

module.exports = {
    install,
    list
};
