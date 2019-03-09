package dk.youtec.drchannels.viewmodel

import android.app.Application
import android.util.Log
import androidx.annotation.Keep
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dk.youtec.drapi.DrMuRepository
import dk.youtec.drapi.MuNowNext
import dk.youtec.drapi.decryptUri
import dk.youtec.drchannels.R
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.koin.core.KoinComponent
import org.koin.core.inject

@Keep
class TvChannelsViewModel(application: Application) : AndroidViewModel(application), KoinComponent {
    private val api: DrMuRepository by inject()
    private val tag = TvChannelsViewModel::class.java.simpleName

    val tvChannels = TvChannels()
    val playbackUri = Channel<String>()
    val error = Channel<String>()

    fun playTvChannel(muNowNext: MuNowNext) {
        viewModelScope.launch {
            try {
                val name = muNowNext.ChannelSlug
                val server = withContext(Dispatchers.IO) {
                    api.getAllActiveDrTvChannels()
                }
                        .first { it.Slug == name }
                        .server() ?: throw Exception("Unable to get streaming server")

                val stream = server
                        .Qualities
                        .sortedByDescending { it.Kbps }.first()
                        .Streams.first().Stream

                playbackUri.send("${server.Server}/$stream")
            } catch (e: Exception) {
                Log.e(tag, e.message, e)
                error.send(if (e.message != null && e.message != "Success") {
                    e.message!!
                } else {
                    getApplication<Application>().getString(R.string.cantChangeChannel)
                })
            }
        }
    }

    fun playProgram(muNowNext: MuNowNext) {
        viewModelScope.launch {
            val uri = muNowNext.Now?.ProgramCard?.PrimaryAsset?.Uri
            if (uri != null) {
                try {
                    val manifest = withContext(Dispatchers.IO) { api.getManifest(uri) }

                    val playbackUri = manifest.getUri() ?: decryptUri(manifest.getEncryptedUri())
                    if (playbackUri.isNotBlank()) {
                        this@TvChannelsViewModel.playbackUri.send(playbackUri)
                    } else {
                        error.send("No stream")
                    }
                } catch (e: Exception) {
                    error.send(
                            if (e.message != null && e.message != "Success") e.message!!
                            else getApplication<Application>().getString(R.string.cantChangeChannel))
                }
            } else {
                error.send("No stream")
            }
        }
    }

    override fun onCleared() {
        Log.d("", "View model was cleared")
        tvChannels.dispose()
        tvChannels.stream.cancel()
        playbackUri.cancel()
        error.cancel()
    }
}

