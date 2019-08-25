package cz.bedla.differ.service

interface DiffRunnerExecutor : AutoCloseable {
    fun start()
    fun scheduleNow(webPageId: String)
}
