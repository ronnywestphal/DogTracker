package se.kth.dogtracker

import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.Toolbar
import androidx.core.graphics.drawable.RoundedBitmapDrawable
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.maps.*
import com.mapbox.maps.plugin.animation.CameraAnimatorChangeListener
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPolylineAnnotationManager
import com.mapbox.maps.plugin.compass.compass
import com.mapbox.maps.plugin.gestures.OnMoveListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.scalebar.scalebar
import com.mapbox.maps.viewannotation.viewAnnotationOptions
import com.mapbox.turf.TurfTransformation
import se.kth.dogtracker.model.DogTracker
import se.kth.dogtracker.model.User
import java.text.SimpleDateFormat
import java.util.*

/**
 * This class displays statistics for a specific track in a map view.
 */
class TrackStatsActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var track: DogTracker

    private lateinit var polylineAnnotationManager: PolylineAnnotationManager
    private var polylineTolerance: Double = 0.00003

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_track_stats)

        // set up custom action bar
        // change title to date time
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.title = getString(R.string.title_activity_tracks)
            actionBar.setDisplayHomeAsUpEnabled(true)
        }
        val user = User.getInstance()
        mapView = findViewById(R.id.mapView)
        polylineAnnotationManager = mapView.annotations.createPolylineAnnotationManager()


        val trackPosition: Int = intent.getIntExtra("position", 0)
        track = user.tracks[trackPosition]
        Log.e("position: ", "" + trackPosition)

        getCity()
        drawPolyLines()
        drawDumbbellAnnotations()
        onMapReady()

        mapView.camera.addCameraZoomChangeListener(onZoomListener)

    }

    private fun onMapReady() {
        mapView.compass.enabled = false
        mapView.scalebar.enabled = false

        mapView.getMapboxMap().setCamera(
            CameraOptions.Builder()
                .zoom(14.5)
                .build()
        )
        mapView.getMapboxMap().loadStyleUri(
            Style.MAPBOX_STREETS
        ) {
            mapView.getMapboxMap().setCamera(CameraOptions.Builder().center(track.dogPoints[0]).build())
            mapView.gestures.addOnMoveListener(onMoveListener)
        }
    }
    private fun drawPolyLines() {

        polylineAnnotationManager.deleteAll()

        val polylineAnnotationOptions: PolylineAnnotationOptions =
            PolylineAnnotationOptions()
                .withPoints(TurfTransformation.simplify(track.trackPoints, polylineTolerance, true))
                .withLineColor(String.format("#%06X", 0xFFFFFF and getColor(R.color.line_human)))
                .withLineWidth(5.0)
        polylineAnnotationManager.create(polylineAnnotationOptions)

        val polylineAnnotationOptionsDog: PolylineAnnotationOptions =
            PolylineAnnotationOptions()
                .withPoints(TurfTransformation.simplify(track.dogPoints, polylineTolerance, true))
                .withLineColor(String.format("#%06X", 0xFFFFFF and getColor(R.color.line_dog)))
                .withLineWidth(5.0)

        // Add line to map.
        polylineAnnotationManager.create(polylineAnnotationOptionsDog)

    }

    private val onZoomListener =
        CameraAnimatorChangeListener<Double> { updatedValue ->
            polylineTolerance =
                if (updatedValue < 13.5 ) 0.00009
                else if (updatedValue < 14.0) 0.00007
                else if (updatedValue < 14.5) 0.00005
                else if (updatedValue < 15.0) 0.00003
                else if (updatedValue < 15.5) 0.00001
                else if (updatedValue < 16.0) 0.000009
                else if (updatedValue < 16.5) 0.000007
                else 0.000005
            drawPolyLines()
        }

    private val onMoveListener = object : OnMoveListener {
        override fun onMoveBegin(detector: MoveGestureDetector) {

        }
        override fun onMove(detector: MoveGestureDetector): Boolean {

            return false
        }
        override fun onMoveEnd(detector: MoveGestureDetector) {

        }
    }

    private fun initUI(){

        var formattedDate = ""
        if (track.trackLocations.size > 0) {
            val date = Date(track.trackLocations[0].time)
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val sdfToday = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val sdfTime = SimpleDateFormat("HH:mm", Locale.getDefault())

            formattedDate = if (sdfToday.format(date).equals(sdfToday.format(System.currentTimeMillis())))
                "Today at " + sdfTime.format(date)
            else sdf.format(date)
            formattedDate += " - $currentPlace"
        }

        updateSelectedDog()
        val dogName = findViewById<AppCompatTextView>(R.id.stats_name)
        dogName.text = track.dog.name
        val timeLocation = findViewById<AppCompatTextView>(R.id.stats_datetime)
        timeLocation.text = formattedDate

        val text1 = findViewById<AppCompatTextView>(R.id.stats_distance)
        val text2 = findViewById<AppCompatTextView>(R.id.stats_time)
        val text3 = findViewById<AppCompatTextView>(R.id.stats_dbs)
        val text4 = findViewById<AppCompatTextView>(R.id.avg_speed)
        text1.text = getString(R.string.distance, track.distance)
        text2.text = getString(R.string.time, track.time/60000, (track.time% 60000) / 1000)
        text3.text = getString(R.string.dumbbells_dog, track.dumbbellsFound, track.dumbbellLocations.size)
        text4.text = getString(R.string.avg_speed, (track.distance / ((track.time) / 1000))* 3.6)

    }
    private fun updateSelectedDog() {
        if (track.dog.bitmapPicture != null) {
            val roundDrawable: RoundedBitmapDrawable = RoundedBitmapDrawableFactory.create(
                resources,
                track.dog.bitmapPicture
            )
            roundDrawable.isCircular = true
            findViewById<AppCompatImageView>(R.id.stats_dog).setImageDrawable(roundDrawable)
        } else
            findViewById<AppCompatImageView>(R.id.stats_dog).setImageResource(R.drawable.empty_profile_picture)
    }
    private val dumbbellAnnotations = arrayListOf<View>()
    /**
     * Draw dumbbell annotations on the map
     */
    private fun drawDumbbellAnnotations() {
        for(view in dumbbellAnnotations)
            mapView.viewAnnotationManager.removeViewAnnotation(view)

        val firstDumbbellIndex = track.dumbbellsFound + track.dumbbellsMissed
        for(i in firstDumbbellIndex until track.dumbbellLocations.size) {
            dumbbellAnnotations.add(mapView.viewAnnotationManager.addViewAnnotation(
                resId = R.layout.annotation_marker,
                options = viewAnnotationOptions {
                    geometry(track.dumbbellLocations[i].point)
                    allowOverlap(true)
                    offsetY(45)
                }
            ))
        }
    }

    // this event will enable the back function to the button on press
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
    private lateinit var currentPlace: String
    private fun getCity(){
        val theUrl = "https://api.mapbox.com/geocoding/v5/mapbox.places/"
        val point = track.trackPoints[0]
        val sb = StringBuilder()
        sb.append(theUrl)
        sb.append(point.longitude())
        sb.append("," + point.latitude() + ".json?")
        sb.append("access_token=sk.eyJ1Ijoicm9ubnl3ZXN0cGhhbCIsImEiOiJjbGJ3NHNsZDUweDBwM3Z0MGQ1ZzlvYjg4In0.ytLhwqk4cb1-GUL3wV59ug")

        val jsonObjectRequest = JsonObjectRequest(Request.Method.GET, sb.toString(), null,
            { response ->
                val jo = response.getJSONArray("features")
                for (i in 0 until jo.length()){
                    val jsonObject = jo.getJSONObject(i)
                    if (jsonObject.get("place_type").toString().contains("region")) {
                        Log.e("place_name ", jsonObject.get("place_name").toString())
                        currentPlace = jsonObject.get("place_name").toString()
                        initUI()
                        break
                    }
                }
            },
            { error ->
                Log.e("Error: ", error.toString())
            }
        )
        Volley.newRequestQueue(this).add(jsonObjectRequest)
    }

}
