import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

fun ShadowJar.addAgentPackage(pkg: String, relocated: String? = null) {
    val attr = "Agent-Packages"
    fun addToAttribute(value: String) {
        if (value !in (manifest.attributes[attr] as? String).orEmpty().split(","))
            manifest.attributes.merge(attr, value) { old, new -> "$old,$new" }
    }

    if (relocated == null || project.hasProperty("no.relocate")) {
        addToAttribute("$pkg.*")
    } else {
        val prefix = "rs_shaded"
        relocate(pkg, "$prefix.$relocated")
        addToAttribute("$prefix.*")
    }
}
