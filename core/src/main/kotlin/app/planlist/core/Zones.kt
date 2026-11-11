package app.planlist.core

object SystemZones {
    const val OTHER = "OTHER"

    fun regionKey(id: String): String = id.substringBefore('/', OTHER)

    fun regions(ids: Collection<String>): List<String> =
        ids.map { regionKey(it) }
            .distinct()
            .sortedWith(compareBy({ it == OTHER }, { it }))

    fun zones(ids: Collection<String>, region: String): List<String> =
        ids.filter { regionKey(it) == region }.sorted()
}
