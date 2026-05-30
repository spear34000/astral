package pics.spear.astral.engine

/** Abstract runtime for executing bot scripts. */
interface Runtime {
    fun initialize()
    fun evaluate(code: String): Any?
    fun callFunction(name: String, vararg args: Any?): Any?
    fun put(key: String, value: Any?)
    fun destroy()
    val isReady: Boolean
}

enum class RuntimeType(val id: String) {
    NODE("node"),
    JAVASCRIPT("javascript"),
    PYTHON("python"),
    ;

    companion object {
        fun from(s: String): RuntimeType =
            entries.find { it.id == s.lowercase() } ?: NODE
    }
}
