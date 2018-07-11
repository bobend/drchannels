package dk.youtec.drchannels.ui

import android.app.ActivityOptions
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.media.tv.Channel
import android.support.v7.app.AppCompatActivity
import android.support.v7.app.AppCompatDelegate
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.view.Menu
import androidx.core.view.isVisible
import dk.youtec.drapi.MuNowNext
import dk.youtec.drchannels.R
import dk.youtec.drchannels.backend.DrMuReactiveRepository
import dk.youtec.drchannels.ui.adapter.ChannelsAdapter
import dk.youtec.drchannels.util.isNullOrEmpty
import dk.youtec.drchannels.viewmodel.ChannelsViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.android.synthetic.main.empty_state.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.toast
import android.support.media.tv.TvContractCompat
import android.content.ContentUris
import android.graphics.BitmapFactory
import android.os.Build
import android.support.media.tv.ChannelLogoUtils
import dk.youtec.drchannels.service.schedulePreviewUpdate
import dk.youtec.drchannels.util.SharedPreferences
import dk.youtec.drchannels.util.putPreference


class MainActivity : AppCompatActivity(), AnkoLogger, ChannelsAdapter.OnChannelClickListener {
    private val api by lazy { DrMuReactiveRepository(this) }

    private lateinit var viewModel: ChannelsViewModel

    private val disposables = CompositeDisposable()

    companion object {
        init {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        initToolbar()

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.addItemDecoration(
                DividerItemDecoration(this, DividerItemDecoration.VERTICAL))

        swipeRefresh.setOnRefreshListener {
            viewModel.channels.subscribe()
        }

        viewModel = ViewModelProviders.of(this)
                .get(ChannelsViewModel::class.java)

        progressBar.isVisible = true

        viewModel.channels.observe(
                this,
                Observer<List<MuNowNext>> { channels ->
                    isEmptyState = channels.isNullOrEmpty()
                    handleChannelsChanged(channels!!)
                    progressBar.isVisible = false
                    swipeRefresh.isRefreshing = false
                })

        //Schedule task
        /*
        val gcmNetworkManager = GcmNetworkManager.getInstance(this)
        gcmNetworkManager.cancelAllTasks(UpcomingProgramsService::class.java)

        val nowTask = OneoffTask.Builder()
                .setService(UpcomingProgramsService::class.java)
                .setExecutionWindow(0L, 10L)
                //.setRequiredNetwork(PeriodicTask.NETWORK_STATE_CONNECTED)
                .setTag(UpcomingProgramsService.UPCOMING_PROGRAMS)
                .build()
        gcmNetworkManager.schedule(nowTask)

        val task = PeriodicTask.Builder()
                .setService(UpcomingProgramsService::class.java)
                .setPeriod(24 * 60 * 60)
                //.setRequiredNetwork(PeriodicTask.NETWORK_STATE_CONNECTED)
                .setTag(UpcomingProgramsService.UPCOMING_PROGRAMS)
                .build()
        gcmNetworkManager.schedule(task)
        */

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && SharedPreferences.getLong(this, "channelId") == 0L) {

            val builder = Channel.Builder()
            // Every channel you create must have the type `TYPE_PREVIEW`
            builder.setType(TvContractCompat.Channels.TYPE_PREVIEW)
                    .setDisplayName(getString(R.string.currentPrograms))
                    .setAppLinkIntent(Intent(this, MainActivity::class.java))

            val channelUri = contentResolver.insert(
                    TvContractCompat.Channels.CONTENT_URI, builder.build().toContentValues())

            val channelId = ContentUris.parseId(channelUri)

            ChannelLogoUtils.storeChannelLogo(this,
                    channelId,
                    BitmapFactory.decodeResource(resources, R.drawable.ic_channel_logo))

            TvContractCompat.requestChannelBrowsable(this, channelId)

            putPreference {
                putLong("channelId", channelId)
            }
        }

        schedulePreviewUpdate()
    }

    /**
     * Called when a change has been observed on channels in ChannelsViewModel
     */
    private fun handleChannelsChanged(channels: List<MuNowNext>) {
        if (!isFinishing) {
            if (recyclerView.adapter != null) {
                (recyclerView.adapter as ChannelsAdapter?)?.submitList(channels)
            } else {
                recyclerView.adapter = ChannelsAdapter(this).apply {
                    submitList(channels)
                }
            }
        }
    }

    private fun initToolbar() {
        toolbar.title = ""
        setSupportActionBar(toolbar)
    }

    override fun onDestroy() {
        super.onDestroy()

        disposables.clear()
    }

    private var isEmptyState: Boolean = false
        set(value) {
            emptyState.isVisible = value
            recyclerView.isVisible = !value
        }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)

        /*
        val listsMenu = menu.findItem(R.id.menu_favourite_lists)
        val subMenu = listsMenu.subMenu
        for ((key, name) in favouriteLists) {
            subMenu.add(listsGroup, key, Menu.NONE, name)
        }
        listsMenu.isVisible = favouriteLists.isNotEmpty()
        */

        return false
    }

    private fun loadGenres() {
        /*
        disposables.add(
                api.retrieveEpgGenres()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeBy(
                                onNext = { genres ->
                                    genreAdapter.clear()
                                    genreAdapter.add(Genre(0, getString(R.string.all)))
                                    genreAdapter.addAll(genres)
                                    genreAdapter.notifyDataSetChanged()
                                },
                                onError = { toast("Error loading genres") }
                        ))
                        */
    }

    override fun playChannel(muNowNext: MuNowNext) {
        val name = muNowNext.ChannelSlug
        disposables.add(
                api.getAllActiveDrTvChannels()
                        .subscribeOn(Schedulers.io())
                        .map { it.first { it.Slug == name } }
                        .map { it.StreamingServers.first { it.LinkType == "HLS" } }
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeBy(
                                onSuccess = { server ->
                                    val stream = server.Qualities.sortedByDescending { it.Kbps }.first().Streams.first().Stream
                                    val playbackUri = "${server.Server}/$stream"
                                    val intent = buildIntent(this@MainActivity, playbackUri)
                                    startActivity(intent)
                                },
                                onError = { e ->
                                    toast(
                                            if (e.message != null
                                                    && e.message != "Success") e.message!!
                                            else getString(R.string.cantChangeChannel))
                                }
                        ))
    }

    override fun playProgram(muNowNext: MuNowNext) {
        val uri = muNowNext.Now?.ProgramCard?.PrimaryAsset?.Uri
        if (uri != null) {
            disposables.add(
                    api.getManifest(uri)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribeBy(
                                    onSuccess = { manifest ->
                                        val playbackUri = manifest.Links.firstOrNull { it.Target == "HLS" }?.Uri
                                        if (playbackUri != null) {
                                            val intent = buildIntent(this@MainActivity, playbackUri)
                                            startActivity(intent)
                                        } else {
                                            toast("No HLS stream")
                                        }
                                    },
                                    onError = { e ->
                                        toast(
                                                if (e.message != null
                                                        && e.message != "Success") e.message!!
                                                else getString(R.string.cantChangeChannel))
                                    }
                            ))
        } else {
            toast("No stream")
        }
    }

    private fun buildIntent(context: Context, uri: String): Intent {
        return Intent(context, PlayerActivity::class.java).apply {
            action = PlayerActivity.ACTION_VIEW
            putExtra(PlayerActivity.PREFER_EXTENSION_DECODERS_EXTRA, false)
            data = Uri.parse(uri)
        }
    }

    override fun showChannel(context: Context, channel: MuNowNext) {
        val intent = Intent(context, ProgramsActivity::class.java).apply {
            putExtra(ProgramsActivity.CHANNEL_NAME, channel.ChannelSlug.toUpperCase())
            putExtra(ProgramsActivity.CHANNEL_ID, channel.ChannelSlug)
        }

        context.startActivity(intent, ActivityOptions.makeCustomAnimation(context,
                R.anim.slide_in_left, R.anim.slide_out_left).toBundle())
    }
}
