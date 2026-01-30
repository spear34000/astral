package pics.spear.astral.runtime

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.*
import java.util.zip.GZIPInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream
import android.system.Os
import android.system.ErrnoException

/**
 * PRoot Manager - Manages the PRoot binary and Linux userland environment
 * 
 * This class is responsible for:
 * 1. Extracting PRoot binary from assets
 * 2. Extracting rootfs (Alpine Linux) from assets
 * 3. Executing commands inside the PRoot environment
 */
class ProotManager(private val context: Context) {
    
    val filesDir = context.filesDir
    internal val userlandDir = File(filesDir, "userland")
    internal val rootfsDir = File(userlandDir, "rootfs")
    internal val tmpDir = File(userlandDir, "tmp")
    internal val prootBinary = File(context.applicationInfo.nativeLibraryDir, "libproot.so")
    internal val prootLoader = File(context.applicationInfo.nativeLibraryDir, "libloader.so")
    private val setupComplete = File(userlandDir, ".setup_complete_v38")
    val nodeExecutablePath: String = "/usr/bin/node"
    val pythonExecutablePath: String = "/usr/bin/python3"

    companion object {
        private const val TAG = "ProotManager"
        private const val PROOT_ASSET = "userland/proot-arm64"
        private const val ROOTFS_PREBUILT_ASSET = "userland/alpine-rootfs-prebuilt.tar.xz"
        private const val ROOTFS_ASSET = "userland/alpine-rootfs.tar.xz"
        private val initMutex = Mutex()
        private const val DESIRED_NODE_MAJOR_PREFIX = "v24."
        private const val DESIRED_NODE_DIST = "latest-v24.x"
        private const val DESIRED_NODE_TARBALL = "node-v24.x-linux-arm64.tar.xz"
        private val NODE_DIST_CANDIDATES = listOf(
            "latest-v24.x",
            "v24.4.0",
            "v24.3.0",
            "v24.2.0",
            "v24.1.0",
            "v24.0.0"
        )
    }
    
    /**
     * Initialize the userland environment
     * This is called on first run or if the setup is incomplete
     */
    suspend fun initialize(): Result<Unit> = initMutex.withLock {
        initializeUnlocked()
    }

    suspend fun resetUserland(): Result<Unit> = initMutex.withLock {
        return@withLock withContext(Dispatchers.IO) {
            try {
                Log.w(TAG, "Resetting userland...")
                if (userlandDir.exists()) {
                    userlandDir.deleteRecursively()
                }
                setupComplete.delete()
                initializeUnlocked()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to reset userland", e)
                Result.failure(e)
            }
        }
    }

    private suspend fun initializeUnlocked(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Initializing PRoot userland (Locked)...")

            // Check if already initialized
            if (setupComplete.exists()) {
                Log.i(TAG, "Userland already initialized")
                ensureResolvConf()
                setupRuntimeEnvironment(runApkUpdate = false)
                return@withContext Result.success(Unit)
            } else {
                Log.i(TAG, "New setup version or incomplete setup detected. Wiping userland for clean installation...")
                if (userlandDir.exists()) {
                    userlandDir.deleteRecursively()
                }
            }

            // Create directories
            userlandDir.mkdirs()
            rootfsDir.mkdirs()
            tmpDir.mkdirs()

            // Extract rootfs
            extractRootfs()

            // Ensure DNS is configured for userland
            ensureResolvConf()

            // Setup runtime environment (Node.js, Python, venv)
            configureEtcHosts()
            setupRuntimeEnvironment(runApkUpdate = true)

            // Mark setup as complete
            setupComplete.createNewFile()

            Log.i(TAG, "Userland initialization complete!")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize userland", e)
            Result.failure(e)
        }
    }
    
    
    /**
     * Extract Alpine Linux rootfs from tar.xz archive
     * If not found in assets, download from Alpine mirrors
     */
    private suspend fun extractRootfs() = withContext(Dispatchers.IO) {
        Log.i(TAG, "Extracting rootfs...")
        
        val assetCandidates = listOf(
            ROOTFS_PREBUILT_ASSET to "prebuilt runtime",
            ROOTFS_ASSET to "base image"
        )

        for ((assetName, label) in assetCandidates) {
            try {
                context.assets.open(assetName).use { input ->
                    extractTarXz(input, rootfsDir)
                }
                Log.i(TAG, "Rootfs extracted from $label asset: $assetName")
                return@withContext
            } catch (e: FileNotFoundException) {
                Log.w(TAG, "Asset $assetName not found: ${e.message}")
            }
        }

        Log.w(TAG, "No rootfs asset found. Downloading from Alpine mirrors...")
        downloadAndExtractRootfs()
    }
    
    /**
     * Download and extract Alpine rootfs
     */
    /**
     * Download and extract Alpine rootfs (musl-based from official mirrors)
     */
    private suspend fun downloadAndExtractRootfs() = withContext(Dispatchers.IO) {
        val preferredVersions = listOf("3.20.3", "3.20.2", "3.20.1", "3.19.1")
        val mirrors = listOf(
            "https://dl-cdn.alpinelinux.org/alpine",
            "https://mirror.clarkson.edu/alpine",
            "https://mirrors.edge.kernel.org/alpine"
        )

        val rootfsUrls = buildList {
            preferredVersions.forEach { version ->
                val parts = version.split('.')
                if (parts.size >= 2) {
                    val major = parts[0]
                    val minor = parts[1]
                    val majorMinorPath = "v$major.$minor"
                    mirrors.forEach { mirror ->
                        add("$mirror/$majorMinorPath/releases/aarch64/alpine-minirootfs-$version-aarch64.tar.gz")
                    }
                }
            }
            mirrors.forEach { mirror ->
                add("$mirror/latest-stable/releases/aarch64/alpine-minirootfs-latest-stable-aarch64.tar.gz")
            }
        }

        var lastError: Exception? = null

        for (rootfsUrl in rootfsUrls) {
            Log.i(TAG, "Attempting to download Alpine rootfs from: $rootfsUrl")
            try {
                val url = java.net.URL(rootfsUrl)
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                connection.connectTimeout = 60000
                connection.readTimeout = 60000

                val responseCode = connection.responseCode
                if (responseCode == 200) {
                    connection.inputStream.use { input ->
                        if (rootfsUrl.endsWith(".gz") || rootfsUrl.endsWith(".tgz")) {
                            extractTarGz(input, rootfsDir)
                        } else {
                            extractTarXz(input, rootfsDir)
                        }
                    }
                    Log.i(TAG, "Alpine rootfs downloaded and extracted successfully from $rootfsUrl")
                    return@withContext
                } else {
                    throw IOException("HTTP $responseCode")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Rootfs download failed from $rootfsUrl", e)
                lastError = e as? Exception ?: Exception(e)
            }
        }

        throw lastError ?: IOException("Failed to download Alpine rootfs from all mirrors")
    }
    
    /**
     * Extract tar.gz archive to a specific directory
     */
    private fun extractTarGz(input: InputStream, targetDir: File, stripComponents: Int = 0) {
        val gzInput = GZIPInputStream(input)
        extractTar(gzInput, targetDir, stripComponents)
    }
    
    /**
     * Extract tar.xz archive to a specific directory
     */
    private fun extractTarXz(input: InputStream, targetDir: File, stripComponents: Int = 0) {
        val xzInput = XZCompressorInputStream(input)
        extractTar(xzInput, targetDir, stripComponents)
    }
    
    /**
     * Extract tar archive (common logic) to a specific directory
     */
    private fun extractTar(input: InputStream, targetDir: File, stripComponents: Int = 0) {
        val tarInput = TarArchiveInputStream(input)
        
        var entry = tarInput.nextTarEntry
        var count = 0
        
        while (entry != null) {
            // Apply stripComponents logic
            val nameParts = entry.name.split("/").filter { it.isNotEmpty() }
            if (nameParts.size > stripComponents) {
                val relativePath = nameParts.drop(stripComponents).joinToString("/")
                val outputFile = File(targetDir, relativePath)
                
                if (entry.isDirectory) {
                    outputFile.mkdirs()
                } else if (entry.isSymbolicLink) {
                    outputFile.parentFile?.mkdirs()
                    try {
                        if (outputFile.exists() || isBrokenSymlink(outputFile)) {
                            outputFile.delete()
                        }
                        Os.symlink(entry.linkName, outputFile.absolutePath)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to create symlink: ${entry.name} -> ${entry.linkName}", e)
                    }
                } else {
                    outputFile.parentFile?.mkdirs()
                    if (outputFile.exists()) outputFile.delete()
                    FileOutputStream(outputFile).use { output ->
                        tarInput.copyTo(output)
                    }
                    
                    // Preserve permissions
                    if (entry.mode and 0x40 != 0) { // Check if executable bit is set
                        outputFile.setExecutable(true, false)
                    }
                }
                count++
            }
            
            if (count % 100 == 0) {
                Log.d(TAG, "Extracted $count files...")
            }
            
            entry = tarInput.nextTarEntry
        }
        
        Log.i(TAG, "Extraction complete! Extracted $count files to ${targetDir.name}.")
    }

    private fun isBrokenSymlink(file: File): Boolean {
        return try {
            Os.lstat(file.absolutePath)
            true // Link exists (broken or not)
        } catch (e: ErrnoException) {
            false
        }
    }
    
    /**
     * Setup Node.js environment using Alpine's package manager
     */
    private suspend fun setupRuntimeEnvironment(runApkUpdate: Boolean) = withContext(Dispatchers.IO) {
        val prebuilt = isPrebuiltRuntime()

        if (!prebuilt) {
            configureApkRepositories()
        }

        Log.i(TAG, "Preparing runtime environment (Node.js & Python)...")

        val runtimeCheck = executeInUserland("command -v node >/dev/null 2>&1 && command -v python3 >/dev/null 2>&1")
        if (runtimeCheck.isFailure) {
            if (prebuilt) {
                Log.w(TAG, "Prebuilt runtime marker found but binaries missing. Falling back to apk installation.")
            } else {
                Log.i(TAG, "Runtime binaries missing. Installing via apk...")
            }

            prepareWritableDirectories()

            val installCommand = if (runApkUpdate) {
                "apk update && apk add --no-cache nodejs npm python3 py3-pip wget tar xz"
            } else {
                "apk add --no-cache nodejs npm python3 py3-pip wget tar xz"
            }

            var installResult: Result<String>? = null
            for (i in 1..3) {
                installResult = executeInUserland(installCommand)
                if (installResult.isSuccess) break
                Log.w(TAG, "apk install failed, retrying... ($i/3)")
                kotlinx.coroutines.delay(2000)
            }
            if (installResult?.isFailure == true) {
                throw installResult.exceptionOrNull() ?: Exception("Failed to install runtime packages via apk after 3 retries")
            }
        } else {
            if (prebuilt) {
                Log.i(TAG, "Prebuilt runtime detected. Skipping apk installation.")
            } else {
                Log.i(TAG, "Runtime packages already present. Skipping apk add.")
            }
        }

        val venvResult = executeInUserland("if [ ! -d /root/venv ]; then python3 -m venv /root/venv; fi")
        if (venvResult.isFailure) {
            throw venvResult.exceptionOrNull() ?: Exception("Failed to ensure Python virtual environment")
        }

        val upgraded = ensureDesiredNodeVersion()
        if (!upgraded) {
            Log.w(TAG, "Node.js upgrade to ${'$'}DESIRED_NODE_DIST may have failed; continuing with existing version.")
        }

        Log.i(TAG, "Runtime environment ready (packages installed, venv ensured).")
    }

    private fun isPrebuiltRuntime(): Boolean {
        return File(rootfsDir, "root/.astral_prebuilt_runtime").exists()
    }

    private suspend fun ensureDesiredNodeVersion(): Boolean {
        val versionProbe = executeInUserland("node -v || true")
        val current = versionProbe.getOrNull()?.trim().orEmpty()
        if (current.startsWith(DESIRED_NODE_MAJOR_PREFIX)) {
            Log.i(TAG, "Node.js already at desired major version: ${'$'}current")
            return true
        }

        Log.i(TAG, "Upgrading Node.js runtime to ${'$'}DESIRED_NODE_DIST (current='${'$'}current').")

        val tools = executeInUserland("command -v wget >/dev/null 2>&1 || apk add --no-cache wget")
        if (tools.isFailure) {
            Log.w(TAG, "Failed to ensure wget availability: ${'$'}{tools.exceptionOrNull()?.message}")
            return false
        }

        val compat = executeInUserland("apk add --no-cache gcompat libstdc++ libc6-compat ca-certificates")
        if (compat.isFailure) {
            Log.w(TAG, "Failed to install glibc compatibility packages: ${'$'}{compat.exceptionOrNull()?.message}")
            return false
        }

        val candidateList = NODE_DIST_CANDIDATES.joinToString(" ")
        val upgradeScript = """
            set -e
            cd /tmp
            python3 - <<'PY'
import hashlib
import json
import os
import platform
import shutil
import ssl
import subprocess
import sys
import tarfile
import urllib.request

PREDEFINED = "${candidateList}".split()
DESIRED_MAJOR = "${DESIRED_NODE_MAJOR_PREFIX}"

context = ssl.create_default_context()

ARCH_ALIAS = {
    "aarch64": "arm64",
    "arm64": "arm64",
    "armv8": "arm64",
    "armv8l": "arm64",
    "armv8b": "arm64",
    "x86_64": "x64",
    "amd64": "x64",
    "x64": "x64",
}

def detect_node_arch():
    machine = platform.machine().lower()
    node_arch = ARCH_ALIAS.get(machine)
    if node_arch:
        return node_arch
    # Fall back to uname if platform.machine() was empty
    try:
        machine = os.uname().machine.lower()
    except Exception:
        machine = ""
    node_arch = ARCH_ALIAS.get(machine)
    if node_arch:
        return node_arch
    print(f"[NodeUpgrade] Unsupported architecture '{machine}'", file=sys.stderr)
    sys.exit(1)

NODE_ARCH = detect_node_arch()
MUSL_TAG = f"linux-{NODE_ARCH}-musl"
GLIBC_TAG = f"linux-{NODE_ARCH}"

def fetch_json(url):
    try:
        with urllib.request.urlopen(url, context=context, timeout=20) as resp:
            return json.loads(resp.read().decode())
    except Exception as exc:
        print(f"[NodeUpgrade] JSON fetch failed {url}: {exc}", file=sys.stderr)
        return []

def fetch_text(url):
    try:
        with urllib.request.urlopen(url, context=context, timeout=20) as resp:
            return resp.read().decode()
    except Exception as exc:
        print(f"[NodeUpgrade] Text fetch failed {url}: {exc}", file=sys.stderr)
        return ""

def parse_sha_map(text):
    mapping = {}
    for line in text.splitlines():
        parts = line.strip().split()
        if len(parts) == 2:
            mapping[parts[1]] = parts[0]
    return mapping

def build_candidates():
    candidates = []
    releases = fetch_json("https://nodejs.org/dist/index.json")
    seen = set()
    for entry in releases:
        version = entry.get("version", "")
        if not version.startswith(DESIRED_MAJOR):
            continue
        files = entry.get("files", [])
        base = f"https://nodejs.org/dist/{version}"
        sha_map = parse_sha_map(fetch_text(f"{base}/SHASUMS256.txt"))
        musl_candidates = [
            f"node-{version}-{MUSL_TAG}.tar.xz",
            f"node-{version}-{MUSL_TAG}.tar.gz"
        ]
        if MUSL_TAG in files:
            for name in musl_candidates:
                if name in sha_map:
                    candidates.append(("release-musl", base, name, sha_map[name]))
                    break
        seen.add(version)

    unofficial_index = fetch_json("https://unofficial-builds.nodejs.org/download/release/index.json")
    for entry in unofficial_index:
        version = entry.get("version", "")
        if not version.startswith(DESIRED_MAJOR):
            continue
        files = entry.get("files", [])
        base = f"https://unofficial-builds.nodejs.org/download/release/{version}"
        sha_map = parse_sha_map(fetch_text(f"{base}/SHASUMS256.txt"))
        if not sha_map:
            continue
        if MUSL_TAG in files:
            for ext in ("tar.xz", "tar.gz"):
                name = f"node-{version}-{MUSL_TAG}.{ext}"
                if name in sha_map:
                    candidates.append(("unofficial-musl", base, name, sha_map[name]))
                    break

    for item in PREDEFINED:
        if not item.startswith("v24."):
            continue
        base = f"https://unofficial-builds.nodejs.org/download/release/{item}"
        sha_map = parse_sha_map(fetch_text(f"{base}/SHASUMS256.txt"))
        if not sha_map:
            continue
        for ext in ("tar.xz", "tar.gz"):
            name = f"node-{item}-{MUSL_TAG}.{ext}"
            if name in sha_map:
                candidates.append(("unofficial-musl", base, name, sha_map[name]))
                break

    nightlies = fetch_json("https://unofficial-builds.nodejs.org/download/nightly/index.json")
    for entry in nightlies:
        version = entry.get("version", "")
        if not version.startswith(DESIRED_MAJOR):
            continue
        files = entry.get("files", [])
        base = f"https://unofficial-builds.nodejs.org/download/nightly/{version}"
        sha_map = parse_sha_map(fetch_text(f"{base}/SHASUMS256.txt"))
        if MUSL_TAG in files:
            for ext in ("tar.xz", "tar.gz"):
                name = f"node-{version}-{MUSL_TAG}.{ext}"
                if name in sha_map:
                    candidates.append(("nightly-musl", base, name, sha_map[name]))
                    break
        break

    return candidates

def download(url, dest):
    if os.path.exists(dest):
        os.remove(dest)
    try:
        subprocess.run(["wget", "-q", "-O", dest, url], check=True, timeout=120)
        return True
    except Exception as exc:
        print(f"[NodeUpgrade] Download failed {url}: {exc}", file=sys.stderr)
        if os.path.exists(dest):
            os.remove(dest)
        return False

def verify_sha(path, expected):
    if not expected:
        return True
    digest = hashlib.sha256()
    with open(path, "rb") as fh:
        for chunk in iter(lambda: fh.read(1 << 20), b""):
            if not chunk:
                break
            digest.update(chunk)
    result = digest.hexdigest()
    if result.lower() != expected.lower():
        print(f"[NodeUpgrade] SHA256 mismatch for {path}: expected {expected} got {result}", file=sys.stderr)
        return False
    return True

def safe_extract(archive_path):
    if not tarfile.is_tarfile(archive_path):
        raise RuntimeError("Downloaded file is not a tar archive")
    with tarfile.open(archive_path, "r:*") as tar:
        members = tar.getmembers()
        if not members:
            raise RuntimeError("Archive is empty")
        top = members[0].name.split("/")[0]
        tar.extractall()
    return top

def install_node(top_dir):
    target_root = "/usr/local/lib/nodejs"
    if os.path.exists(target_root):
        shutil.rmtree(target_root)
    shutil.move(top_dir, target_root)
    bin_dir = os.path.join(target_root, "bin")
    if not os.path.exists(os.path.join(bin_dir, "node")):
        raise RuntimeError("Node.js binary missing after extraction")
    link_targets = ["/usr/local/bin", "/usr/bin"]
    for link_dir in link_targets:
        os.makedirs(link_dir, exist_ok=True)
        for tool in ("node", "npm", "npx", "corepack"):
            src = os.path.join(bin_dir, tool)
            if not os.path.exists(src):
                continue
            dst = os.path.join(link_dir, tool)
            try:
                if os.path.islink(dst) or os.path.exists(dst):
                    os.remove(dst)
            except FileNotFoundError:
                pass
            os.symlink(src, dst)

def ensure_toolchain(packages):
    os.makedirs(TOOLCHAIN_ROOT, exist_ok=True)
    marker = os.path.join(TOOLCHAIN_ROOT, ".initialized")
    cmd = [
        "apk",
        "--root", TOOLCHAIN_ROOT,
        "--keys-dir", "/etc/apk/keys",
        "--repositories-file", "/etc/apk/repositories",
        "--no-cache",
        "--update-cache",
    ]
    if not os.path.exists(marker):
        cmd.append("--initdb")
    subprocess.run(cmd + ["add"] + packages, check=True)
    with open(marker, "w") as fh:
        fh.write("ready")
    return TOOLCHAIN_ROOT

def setup_build_env(root_path):
    env = os.environ.copy()
    bin_paths = [
        os.path.join(root_path, "usr", "local", "bin"),
        os.path.join(root_path, "usr", "bin"),
        os.path.join(root_path, "bin"),
        env.get("PATH", "")
    ]
    env["PATH"] = ":".join([p for p in bin_paths if p])
    gcc_path = os.path.join(root_path, "usr", "bin", "gcc")
    gxx_path = os.path.join(root_path, "usr", "bin", "g++")
    if os.path.exists(gcc_path):
        env.setdefault("CC", gcc_path)
    if os.path.exists(gxx_path):
        env.setdefault("CXX", gxx_path)
    lib_paths = [
        os.path.join(root_path, "usr", "lib"),
        os.path.join(root_path, "lib"),
        env.get("LD_LIBRARY_PATH", "")
    ]
    env["LD_LIBRARY_PATH"] = ":".join([p for p in lib_paths if p])
    pkg_paths = [
        os.path.join(root_path, "usr", "lib", "pkgconfig"),
        os.path.join(root_path, "usr", "share", "pkgconfig"),
        env.get("PKG_CONFIG_PATH", "")
    ]
    env["PKG_CONFIG_PATH"] = ":".join([p for p in pkg_paths if p])
    return env

def main():
    candidates = build_candidates()
    if not candidates:
        print("[NodeUpgrade] No candidates found for Node.js v24", file=sys.stderr)
        sys.exit(1)

    musl_candidates = [c for c in candidates if c[0].endswith("musl")]
    if not musl_candidates:
        print("[NodeUpgrade] No musl-compatible Node.js builds available", file=sys.stderr)
        sys.exit(1)

    successful = False
    for kind, base, archive, sha256 in musl_candidates:
        url = f"{base}/{archive}"
        print(f"[NodeUpgrade] Installing {kind} -> {url}", file=sys.stderr)
        if not download(url, archive):
            continue
        if not verify_sha(archive, sha256):
            continue
        top_dir = None
        try:
            top_dir = safe_extract(archive)
            install_node(top_dir)
            node_path = "/usr/local/lib/nodejs/bin/node"
            completed = subprocess.run([node_path, "-v"], check=True, capture_output=True, text=True)
            print(f"[NodeUpgrade] Node.js installed: {completed.stdout.strip()}")
            successful = True
            break
        except Exception as exc:
            print(f"[NodeUpgrade] Candidate failed verification {kind}: {exc}", file=sys.stderr)
            if os.path.exists("/usr/local/lib/nodejs"):
                shutil.rmtree("/usr/local/lib/nodejs")
        finally:
            try:
                if top_dir and os.path.exists(top_dir):
                    shutil.rmtree(top_dir, ignore_errors=True)
            except Exception:
                pass
            try:
                os.remove(archive)
            except OSError:
                pass

    if not successful:
        print("[NodeUpgrade] Exhausted musl candidates without success", file=sys.stderr)
        sys.exit(1)

if __name__ == "__main__":
    main()
PY
            ln -sf /usr/local/lib/nodejs/bin/npx /usr/local/bin/npx
            ln -sf /usr/local/lib/nodejs/bin/corepack /usr/local/bin/corepack
            ln -sf /usr/local/lib/nodejs/bin/node /usr/bin/node
            ln -sf /usr/local/lib/nodejs/bin/npm /usr/bin/npm
            ln -sf /usr/local/lib/nodejs/bin/npx /usr/bin/npx
        """.trimIndent()

        val upgradeResult = executeInUserland(upgradeScript)
        if (upgradeResult.isFailure) {
            Log.w(TAG, "Node.js download or install failed: ${'$'}{upgradeResult.exceptionOrNull()?.message}")
            return false
        }

        val verify = executeInUserland("node -v")
        val verifiedVersion = verify.getOrNull()?.trim().orEmpty()
        val success = verify.isSuccess && verifiedVersion.startsWith(DESIRED_NODE_MAJOR_PREFIX)
        if (success) {
            Log.i(TAG, "Node.js runtime upgraded successfully to $verifiedVersion")
        } else {
            Log.w(TAG, "Node.js upgrade verification failed, reported version='${'$'}verifiedVersion'")
        }
        return success
    }

    private suspend fun prepareWritableDirectories() {
        val setup = executeInUserland(
            "rm -rf /etc/terminfo && rm -rf /usr/share/terminfo && mkdir -p /etc/terminfo && mkdir -p /usr/share/terminfo && chmod 755 /etc/terminfo /usr/share/terminfo && chown -R root:root /etc/terminfo /usr/share/terminfo"
        )
        if (setup.isFailure) {
            Log.w(TAG, "Failed to pre-create terminfo directories: ${setup.exceptionOrNull()?.message}")
        }
    }
    
    /**
     * Execute a command inside the PRoot environment
     */
    suspend fun executeInUserland(
        command: String,
        link2symlink: Boolean = true,
        extraBinds: List<Pair<File, String>> = emptyList()
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (!prootBinary.exists()) {
                throw FileNotFoundException("PRoot binary not found")
            }
            
            if (!rootfsDir.exists() || !rootfsDir.isDirectory) {
                throw FileNotFoundException("Rootfs not found")
            }

            ensureResolvConf()
            
            // Build PRoot command
            val prootCommand = buildList {
                add(prootBinary.absolutePath)
                add("-0") // Root user emulation
                if (link2symlink) {
                    add("-p") // link2symlink - Helps with EROFS and some syscalls
                }
                add("-k") // Fake kernel version
                add("4.19.0")
                add("-r")
                add(rootfsDir.absolutePath)
                add("-b")
                add("/dev")
                add("-b")
                add("/proc")
                add("-b")
                add("/sys")
                add("-b")
                add("${tmpDir.absolutePath}:/tmp")
                extraBinds.forEach { (host, guest) ->
                    add("-b")
                    add("${host.absolutePath}:$guest")
                }
                add("-w")
                add("/root")
                add("/bin/sh")
                add("-c")
                add(withVenv(command))
            }
            
            Log.d(TAG, "Executing: ${prootCommand.joinToString(" ")}")
            
            val processBuilder = ProcessBuilder(prootCommand)
            val env = processBuilder.environment()
            env["PROOT_TMP_DIR"] = tmpDir.absolutePath
            env["TMPDIR"] = tmpDir.absolutePath
            env["PROOT_LOADER"] = prootLoader.absolutePath
            env["PATH"] = "/usr/bin:/usr/local/bin:/usr/local/sbin:/usr/sbin:/sbin:/bin"
            
            // Set host LD_LIBRARY_PATH so PRoot can find libtalloc.so
            env["LD_LIBRARY_PATH"] = context.applicationInfo.nativeLibraryDir

            processBuilder.redirectErrorStream(true)
            
            val process = processBuilder.start()
            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()
            
            if (exitCode == 0) {
                Log.d(TAG, "Command executed successfully")
                Result.success(output)
            } else {
                Log.e(TAG, "Command failed with exit code $exitCode: $output")
                Result.failure(Exception("Command failed: $output"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to execute command", e)
            Result.failure(e)
        }
    }
    
    /**
     * Check if userland is initialized
     */
    fun isInitialized(): Boolean {
        return setupComplete.exists() && 
               prootBinary.exists() && prootBinary.length() > 0 && 
               rootfsDir.exists()
    }
    
    /**
     * Get Node.js version
     */
    suspend fun getNodeVersion(): Result<String> {
        return executeInUserland("node --version")
    }

    /**
     * Execute a command inside userland and stream output lines
     */
    suspend fun executeInUserlandStreaming(
        command: String,
        link2symlink: Boolean = true,
        extraBinds: List<Pair<File, String>> = emptyList(),
        onLine: (String) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!prootBinary.exists()) {
                throw FileNotFoundException("PRoot binary not found")
            }

            if (!rootfsDir.exists() || !rootfsDir.isDirectory) {
                throw FileNotFoundException("Rootfs not found")
            }

            ensureResolvConf()

            val prootCommand = buildList {
                add(prootBinary.absolutePath)
                add("-0")
                if (link2symlink) {
                    add("-p")
                }
                add("-k")
                add("4.19.0")
                add("-r")
                add(rootfsDir.absolutePath)
                add("-b")
                add("/dev")
                add("-b")
                add("/proc")
                add("-b")
                add("/sys")
                add("-b")
                add("${tmpDir.absolutePath}:/tmp")
                extraBinds.forEach { (host, guest) ->
                    add("-b")
                    add("${host.absolutePath}:$guest")
                }
                add("-w")
                add("/root")
                add("/bin/sh")
                add("-c")
                add(withVenv(command))
            }

            Log.d(TAG, "Executing (stream): ${prootCommand.joinToString(" ")}")

            val processBuilder = ProcessBuilder(prootCommand)
            val env = processBuilder.environment()
            env["PROOT_TMP_DIR"] = tmpDir.absolutePath
            env["TMPDIR"] = tmpDir.absolutePath
            env["PROOT_LOADER"] = prootLoader.absolutePath
            env["PATH"] = "/usr/bin:/usr/local/bin:/usr/local/sbin:/usr/sbin:/sbin:/bin"
            env["LD_LIBRARY_PATH"] = context.applicationInfo.nativeLibraryDir

            processBuilder.redirectErrorStream(true)
            val process = processBuilder.start()
            try {
                process.inputStream.bufferedReader().forEachLine { line ->
                    onLine(line)
                }
            } finally {
                val exitCode = process.waitFor()
                if (exitCode != 0) {
                    return@withContext Result.failure(Exception("Command failed with exit code $exitCode"))
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to execute command (stream)", e)
            Result.failure(e)
        }
    }

    internal fun withVenv(command: String): String {
        return "if [ -f /root/venv/bin/activate ]; then . /root/venv/bin/activate; fi; $command"
    }

    private fun ensureResolvConf() {
        try {
            val etcDir = File(rootfsDir, "etc")
            if (!etcDir.exists()) etcDir.mkdirs()
            val resolv = File(etcDir, "resolv.conf")

            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            val activeNetwork = cm.activeNetwork
            val linkProperties = cm.getLinkProperties(activeNetwork)
            val dynamicDnsServers = linkProperties?.dnsServers?.map { it.hostAddress } ?: emptyList()

            val defaultDnsServers = listOf(
                "8.8.8.8",      // Google
                "1.1.1.1",      // Cloudflare
                "9.9.9.9"       // Quad9
            )

            val allDnsServers = (dynamicDnsServers + defaultDnsServers).distinct().filterNotNull()

            if (allDnsServers.isNotEmpty()) {
                val dnsConfig = allDnsServers.joinToString("\n") { "nameserver $it" }
                resolv.writeText(dnsConfig + "\n")
                Log.i(TAG, "Successfully wrote /etc/resolv.conf with DNS servers: ${allDnsServers.joinToString()}")
            } else {
                Log.w(TAG, "No DNS servers found. Using fallback.")
                resolv.writeText("nameserver 8.8.8.8\nnameserver 1.1.1.1\n")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to set resolv.conf, using fallback: ${e.message}")
            val resolv = File(rootfsDir, "etc/resolv.conf")
            resolv.writeText("nameserver 8.8.8.8\nnameserver 1.1.1.1\n")
        }
    }

    /**
     * Ensure Python3 + pip are available in userland
     */
    suspend fun ensurePythonEnvironment(): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isInitialized()) {
            val init = initialize()
            if (init.isFailure) return@withContext init
        }
        if (isPythonAvailable()) {
            Log.i(TAG, "Python already installed.")
            return@withContext Result.success(Unit)
        }

        Log.i(TAG, "Python not found, installing...")
        return@withContext setupPythonEnvironment()
    }

    private suspend fun setupPythonEnvironment(): Result<Unit> = withContext(Dispatchers.IO) {
        Log.i(TAG, "Setting up Python environment using apk...")
        val command = "apk update && apk add --no-cache python3 py3-pip"
        val result = executeInUserland(command)
        if (result.isFailure) {
            return@withContext Result.failure(result.exceptionOrNull() ?: Exception("Failed to install Python using apk"))
        }
        // Create symlink for python -> python3 if it doesn't exist
        executeInUserland("ln -sf /usr/bin/python3 /usr/bin/python")
        Log.i(TAG, "Python installed successfully via apk.")
        return@withContext Result.success(Unit)
    }

    internal suspend fun isPythonAvailable(): Boolean {
        val pythonCheck = executeInUserland("python3 --version")
        return pythonCheck.isSuccess
    }

    private fun configureEtcHosts() {
        val hostsFile = File(rootfsDir, "etc/hosts")
        try {
            val hostEntries = listOf(
                "151.101.110.133 dl-cdn.alpinelinux.org",
                "146.75.122.133 sg.alpinelinux.org",
                "210.92.38.164 kr.alpinelinux.org"
            ).joinToString("\n")

            val existingContent = if (hostsFile.exists()) hostsFile.readText() else ""
            val newContent = buildString {
                appendLine("127.0.0.1 localhost")
                appendLine("::1 ip6-localhost")
                appendLine(hostEntries)
                // Keep other existing entries that are not related to our mirrors
                existingContent.lines().forEach { line ->
                    if (line.isNotBlank() && !line.contains("alpinelinux.org") && !line.contains("localhost")) {
                        appendLine(line)
                    }
                }
            }
            hostsFile.writeText(newContent)
            Log.i(TAG, "Configured /etc/hosts with mirror IPs.")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to configure /etc/hosts", e)
        }
    }

    private fun configureApkRepositories() {
        val repositoriesFile = File(rootfsDir, "etc/apk/repositories")
        try {
            val mirrors = listOf(
                "https://dl-cdn.alpinelinux.org/alpine/v3.20/main",
                "https://dl-cdn.alpinelinux.org/alpine/v3.20/community",
                "https://sg.alpinelinux.org/alpine/v3.20/main",
                "https://sg.alpinelinux.org/alpine/v3.20/community",
                "https://kr.alpinelinux.org/alpine/v3.20/main",
                "https://kr.alpinelinux.org/alpine/v3.20/community"
            ).joinToString("\n")
            repositoriesFile.writeText(mirrors)
            Log.i(TAG, "Configured APK repositories with multiple mirrors.")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to configure APK repositories", e)
        }
    }

}
