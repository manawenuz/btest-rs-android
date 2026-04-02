package com.btestrs.android

data class BtestConfig(
    val host: String = "104.225.217.60",
    val username: String = "btest",
    val password: String = "btest",
    val protocol: Protocol = Protocol.TCP,
    val direction: Direction = Direction.BOTH,
    val duration: Int = 30
) {
    enum class Protocol { TCP, UDP }
    enum class Direction { SEND, RECEIVE, BOTH }

    fun toCommandArgs(binaryPath: String): List<String> = buildList {
        add(binaryPath)
        add("-c"); add(host)
        add("-a"); add(username)
        add("-p"); add(password)
        if (protocol == Protocol.UDP) add("-u")
        when (direction) {
            Direction.SEND -> add("-t")
            Direction.RECEIVE -> add("-r")
            Direction.BOTH -> { add("-t"); add("-r") }
        }
        add("-d"); add(duration.toString())
    }
}
