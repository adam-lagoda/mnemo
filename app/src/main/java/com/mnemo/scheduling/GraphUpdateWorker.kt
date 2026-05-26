package com.mnemo.scheduling

import android.content.Context
import androidx.work.*
import com.mnemo.di.AppModule
import com.mnemo.graph.LouvainClustering

class GraphUpdateWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = AppModule.getInstance(applicationContext)
        val screenshotRepo = app.screenshotRepository
        val graphRepo = app.graphRepository
        val graphBuilder = app.graphBuilder
        val louvain = LouvainClustering()
        val analytics = app.graphAnalytics

        val edges = graphBuilder.buildEdges()
        graphRepo.deleteAll()
        if (edges.isNotEmpty()) graphRepo.insertAll(edges)

        val screenshots = screenshotRepo.getAll()
        if (screenshots.isNotEmpty()) {
            val communities = louvain.cluster(
                nodes = screenshots.map { it.id },
                edges = edges.map { Triple(it.sourceId, it.targetId, it.weight) }
            )
            communities.forEach { (id, communityId) ->
                screenshotRepo.updateCommunity(id, communityId)
            }
        }
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "graph_update"

        fun enqueue(context: Context) {
            val request = OneTimeWorkRequestBuilder<GraphUpdateWorker>().build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}
