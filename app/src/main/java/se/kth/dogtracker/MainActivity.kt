package se.kth.dogtracker

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.Group
import androidx.core.graphics.drawable.RoundedBitmapDrawable
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.ScreenCoordinate
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.expressions.dsl.generated.interpolate
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.animation.*
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotation
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPolylineAnnotationManager
import com.mapbox.maps.plugin.compass.compass
import com.mapbox.maps.plugin.gestures.OnMoveListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorBearingChangedListener
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.plugin.scalebar.scalebar
import com.mapbox.maps.viewannotation.ViewAnnotationUpdateMode
import com.mapbox.maps.viewannotation.viewAnnotationOptions
import com.mapbox.turf.TurfTransformation
import se.kth.dogtracker.io.DatabaseIO
import se.kth.dogtracker.model.DogTracker
import se.kth.dogtracker.model.User
import se.kth.dogtracker.utils.LocationPermissionHelper
import se.kth.dogtracker.utils.VibrationUtil
import java.lang.ref.WeakReference

class MainActivity : AppCompatActivity() {

    private lateinit var dogTracker: DogTracker

    private lateinit var locationPermissionHelper: LocationPermissionHelper
    private lateinit var mapView: MapView

    private var cameraManuallyMoved: Boolean = false
    private var cameraZoomed: Boolean = false
    private lateinit var lastPosition: Point
    private var lastBearing: Double = 0.0

    private lateinit var startAnnotation: View
    private lateinit var endAnnotation: View

    private val dumbbellAnnotations = arrayListOf<View>()
    private lateinit var auth: FirebaseAuth
    private lateinit var currentUser: User

    private lateinit var sp: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = Firebase.auth
        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            return
        } else {
            setContentView(R.layout.activity_main)
            loadCurrentUser()
        }

        sp = getSharedPreferences("sharedPrefs", MODE_PRIVATE)
        mapView = findViewById(R.id.mapView)
        dogTracker = DogTracker()
        initUI()

        locationPermissionHelper = LocationPermissionHelper(WeakReference(this))
        locationPermissionHelper.checkPermissions {
            onMapReady()
        }
    }

    private val onCameraAnchorChange =
        CameraAnimatorNullableChangeListener<ScreenCoordinate?> {
            if (cameraManuallyMoved)
                mapView.gestures.focalPoint = it
    }

    private val onIndicatorBearingChangedListener = OnIndicatorBearingChangedListener {
        lastBearing = it

        if(!cameraManuallyMoved)
            mapView.getMapboxMap().setCamera(CameraOptions.Builder().bearing(it).build())
    }

    private val onIndicatorPositionChangedListener = OnIndicatorPositionChangedListener{
        if (!cameraManuallyMoved)
            mapView.gestures.focalPoint = mapView.getMapboxMap().pixelForCoordinate(it)

        lastPosition = it

        // center camera immediately when indicator moves
        if(!cameraManuallyMoved)
            mapView.getMapboxMap().setCamera(CameraOptions.Builder().center(it).build())

        // tell the dog tracker model that the phone's location was updated
        if (dogTracker.onPositionChanged(it)) {
            drawPolyLines()
        }
        updateTrackingStatisticsUI()
    }

    private lateinit var polylineAnnotationManager: PolylineAnnotationManager
    private lateinit var dogLine: PolylineAnnotation
    private lateinit var humanLine: PolylineAnnotation
    private var polylineTolerance: Double = 0.00001

    /**
     * Initiates the PolylineAnnotationManager and two Polyline objects.
     */
    private fun initPolyLines() {
        polylineAnnotationManager = mapView.annotations.createPolylineAnnotationManager()
        dogLine = polylineAnnotationManager.create(
            PolylineAnnotationOptions().withPoints(emptyList()))
        humanLine = polylineAnnotationManager.create(
            PolylineAnnotationOptions().withPoints(emptyList()))
    }

    /**
     * Draws poly lines on the map using Point objects. The resolution changes depending
     * on the current zoom level.
     */
    private fun drawPolyLines() {
        if (dogTracker.state == DogTracker.State.HUMAN || cameraZoomed) {
            cameraZoomed = false
            val polylineAnnotationOptions: PolylineAnnotationOptions =
                PolylineAnnotationOptions()
                    .withPoints(TurfTransformation.simplify(dogTracker.trackPoints, polylineTolerance, true))
                    .withLineColor(String.format("#%06X", 0xFFFFFF and getColor(R.color.line_human)))
                    .withLineWidth(5.0)
            if (humanLine.points.isNotEmpty())
                polylineAnnotationManager.delete(humanLine)
            else polylineAnnotationManager.deleteAll()
            humanLine = polylineAnnotationManager.create(polylineAnnotationOptions)
        }

        if (dogTracker.state == DogTracker.State.DOG) {
            val polylineAnnotationOptions: PolylineAnnotationOptions =
                PolylineAnnotationOptions()
                    .withPoints(TurfTransformation.simplify(dogTracker.dogPoints, polylineTolerance, true))
                    .withLineColor(String.format("#%06X", 0xFFFFFF and getColor(R.color.line_dog)))
                    .withLineWidth(5.0)
            if (dogLine.points.isNotEmpty())
                polylineAnnotationManager.delete(dogLine)

            dogLine = polylineAnnotationManager.create(polylineAnnotationOptions)
        }
    }

    private fun onMapReady() {
        mapView.compass.enabled = false
        mapView.scalebar.enabled = false
        mapView.camera.addCameraZoomChangeListener(onZoomListener)
        mapView.camera.addCameraAnchorChangeListener(onCameraAnchorChange)
        mapView.getMapboxMap().setCamera(
            CameraOptions.Builder()
                .zoom(16.0)
                .build()
        )
        mapView.getMapboxMap().loadStyleUri(
            sp.getString("mapStyle", Style.SATELLITE).toString()
        ) {
            initLocationComponent()
            setupGesturesListener()
        }
    }

    /**
     * The current zoom-level decides the resolution of the poly lines
     */
    private val onZoomListener =
        CameraAnimatorChangeListener<Double> { updatedValue ->
            if (updatedValue % 0.5 == 0.0){
                cameraZoomed = true
                //zoomValue = updatedValue
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
        }

    private val onMoveListener = object : OnMoveListener {
        override fun onMoveBegin(detector: MoveGestureDetector) {
            cameraManuallyMoved = true
        }
        override fun onMove(detector: MoveGestureDetector): Boolean {
            return false
        }
        override fun onMoveEnd(detector: MoveGestureDetector) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.location
            .removeOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener)
        mapView.location
            .removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
        mapView.gestures.removeOnMoveListener(onMoveListener)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        locationPermissionHelper.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
    private fun setupGesturesListener() {
        mapView.gestures.addOnMoveListener(onMoveListener)
    }
    private fun initLocationComponent() {
        val locationComponentPlugin = mapView.location
        locationComponentPlugin.updateSettings {
            this.enabled = true
            this.locationPuck = LocationPuck2D(
                scaleExpression = interpolate {
                    linear()
                    zoom()
                    stop {
                        literal(0.0)
                        literal(0.6)
                    }
                    stop {
                        literal(20.0)
                        literal(1.0)
                    }
                }.toJson()
            )
        }
        updateLocationPuck()
        initPolyLines()
        locationComponentPlugin.addOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
        locationComponentPlugin.addOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener)
    }

    /**
     * Update the location pucks looks depending on if it's the human or dog that is walking the track.
     */
    private fun updateLocationPuck() {
        var puck: Drawable? = AppCompatResources.getDrawable(this@MainActivity, R.drawable.puck_human)

        if(dogTracker.state == DogTracker.State.DOG || dogTracker.state == DogTracker.State.START_DOG)
            puck = AppCompatResources.getDrawable(this@MainActivity, R.drawable.puck_dog)

        mapView.location.updateSettings {
            this.locationPuck = LocationPuck2D(
                bearingImage = puck
            )
        }
    }

    /**
     * Draw dumbbell annotations on the map.
     */
    private fun drawDumbbellAnnotations() {
        for(view in dumbbellAnnotations)
            mapView.viewAnnotationManager.removeViewAnnotation(view)

        val firstDumbbellIndex = dogTracker.dumbbellsFound + dogTracker.dumbbellsMissed
        for(i in firstDumbbellIndex until dogTracker.dumbbellLocations.size) {
            dumbbellAnnotations.add(mapView.viewAnnotationManager.addViewAnnotation(
                resId = R.layout.annotation_marker,
                options = viewAnnotationOptions {
                    geometry(dogTracker.dumbbellLocations[i].point)
                    allowOverlap(true)
                    offsetY(45)
                }
            ))
        }
    }

    private lateinit var viewStartHuman: View
    private lateinit var viewHuman: View
    private lateinit var viewStartDog: View
    private lateinit var viewDog: View

    private lateinit var buttonPause: AppCompatImageButton
    private lateinit var buttonResume: AppCompatImageButton

    private lateinit var textDistance: AppCompatTextView
    private lateinit var textTime: AppCompatTextView
    private lateinit var textDumbbells: AppCompatTextView

    private lateinit var groupDumbbell: Group

    private fun initUI(){
        // drawer stuff
        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)
        findViewById<View>(R.id.button_menu).setOnClickListener { drawerLayout.open() }

        // side navigation view stuff
        val navigationView = findViewById<NavigationView>(R.id.side_navigation)
        navigationView.setNavigationItemSelectedListener(this::navigationListener)

        // tracking ui stuff
        viewStartHuman = findViewById(R.id.group_start_human)
        viewHuman = findViewById(R.id.group_human)
        viewStartDog = findViewById(R.id.group_start_dog)
        viewDog = findViewById(R.id.group_dog)
        textDistance = findViewById(R.id.text_distance)
        textTime = findViewById(R.id.text_time)
        textDumbbells = findViewById(R.id.text_dumbbells)
        buttonPause = findViewById(R.id.button_pause)
        buttonResume = findViewById(R.id.button_resume)
        groupDumbbell = findViewById(R.id.group_dumbbell)

        // button listeners
        val viewAnnotationManager = mapView.viewAnnotationManager
        viewAnnotationManager.setViewAnnotationUpdateMode(ViewAnnotationUpdateMode.MAP_SYNCHRONIZED)

        findViewById<View>(R.id.button_start_human).setOnLongClickListener {
            val user = User.getInstance()
            if (user.dogs.dogs.size == 0) {
                Toast.makeText(this, "Please create a dog first", Toast.LENGTH_SHORT).show()
            }  else {
                VibrationUtil.vibrate(this)
                polylineAnnotationManager.deleteAll()
                dogTracker.startTrack(lastPosition)
                mapView.location.removeOnIndicatorBearingChangedListener(
                    onIndicatorBearingChangedListener
                )

                // set start annotation
                startAnnotation = mapView.viewAnnotationManager.addViewAnnotation(
                    resId = R.layout.annotation_start,
                    options = viewAnnotationOptions {
                        geometry(dogTracker.trackPoints[0])
                        allowOverlap(true)
                        offsetY(45)
                    }
                )
                updateTrackingUI()
                updateLocationPuck()
            }

            true
        }

        findViewById<View>(R.id.button_end_human).setOnLongClickListener {
            VibrationUtil.vibrate(this)
            dogTracker.stopTrack(lastPosition)

            // set end annotation
            endAnnotation = mapView.viewAnnotationManager.addViewAnnotation(
                resId = R.layout.annotation_end,
                options = viewAnnotationOptions {
                    geometry(dogTracker.trackPoints[dogTracker.trackPoints.size - 1])
                    allowOverlap(true)
                    offsetY(45)
                }
            )
            updateTrackingUI()
            updateLocationPuck()

            true
        }

        findViewById<View>(R.id.button_pause).setOnClickListener {
            Toast.makeText(this, "Tracking paused", Toast.LENGTH_SHORT).show()
            dogTracker.pauseTrack()
            updateTrackingUI()
        }

        findViewById<View>(R.id.button_resume).setOnClickListener {
            Toast.makeText(this, "Tracking resumed", Toast.LENGTH_SHORT).show()
            dogTracker.resumeTrack()

            updateTrackingUI()
        }

        findViewById<View>(R.id.button_marker).setOnClickListener {
            dogTracker.addDumbbellToTrack()
            drawDumbbellAnnotations()
            updateTrackingUI()
            Toast.makeText(this, "Dumbbell added", Toast.LENGTH_SHORT).show()
        }

        findViewById<View>(R.id.button_start_dog).setOnLongClickListener {
            VibrationUtil.vibrate(this)
            dogTracker.startTrack(lastPosition)
            updateTrackingUI()
            updateLocationPuck()

            true
        }

        findViewById<View>(R.id.button_end_dog).setOnLongClickListener {
            VibrationUtil.vibrate(this)
            dogTracker.stopTrack(lastPosition)

            if(::startAnnotation.isInitialized)
                mapView.viewAnnotationManager.removeViewAnnotation(startAnnotation)
            if(::endAnnotation.isInitialized)
                mapView.viewAnnotationManager.removeViewAnnotation(endAnnotation)

            updateTrackingUI()
            updateLocationPuck()

            Log.e("MainActivity", currentUser.tracks.size.toString())


            startActivity(Intent(this, TrackStatsActivity::class.java).apply {
                putExtra("position", currentUser.tracks.size - 1)
            })

            true
        }

        findViewById<View>(R.id.button_found).setOnClickListener {
            dogTracker.markDumbbellFoundOrMissed(true)
            drawDumbbellAnnotations()
            updateTrackingUI()
        }

        findViewById<View>(R.id.button_missed).setOnClickListener {
            dogTracker.markDumbbellFoundOrMissed(false)
            drawDumbbellAnnotations()
            updateTrackingUI()
        }

        // center position and bearing
        findViewById<View>(R.id.button_center_camera).setOnClickListener {
            // TODO: ugly hack!!!!
            if(::lastPosition.isInitialized) {
                mapView.getMapboxMap().easeTo(CameraOptions.Builder().center(lastPosition).bearing(lastBearing).build())
                object: CountDownTimer(1000, 1000) {
                    override fun onTick(millisUntilFinished: Long) { }
                    override fun onFinish() { cameraManuallyMoved = false }
                }.start()
            }
        }

        findViewById<View>(R.id.button_select_map).setOnClickListener {
            val popup = PopupMenu(this, it, Gravity.END, 0, R.style.PopupMenu)
            var styleSelect = ""
            popup.menuInflater.inflate(R.menu.map_styles_popup, popup.menu)
            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.popup_satellite ->
                        styleSelect = Style.SATELLITE
                    R.id.popup_streets ->
                        styleSelect = Style.MAPBOX_STREETS
                    R.id.popup_satellite_streets ->
                        styleSelect = Style.SATELLITE_STREETS
                    R.id.popup_dark ->
                        styleSelect = Style.DARK
                    R.id.popup_light ->
                        styleSelect = Style.LIGHT
                }

                val editor = getSharedPreferences("sharedPrefs", MODE_PRIVATE).edit()
                editor.putString("mapStyle", styleSelect)
                editor.apply()
                mapView.getMapboxMap().loadStyleUri(styleSelect)
                true
            }
            popup.show()
        }

        updateTrackingUI()
    }

    // update tracking UI depending on what the model says
    private fun updateTrackingUI() {
        viewStartHuman.visibility = View.GONE
        viewHuman.visibility = View.GONE
        viewStartDog.visibility = View.GONE
        viewDog.visibility = View.GONE
        textDistance.visibility = View.GONE
        textTime.visibility = View.GONE
        textDumbbells.visibility = View.GONE
        groupDumbbell.visibility = View.GONE

        if(dogTracker.state == DogTracker.State.START_HUMAN) {
            viewStartHuman.visibility = View.VISIBLE
        }
        else if(dogTracker.state == DogTracker.State.HUMAN) {
            viewHuman.visibility = View.VISIBLE
            textDistance.visibility = View.VISIBLE
            textTime.visibility = View.VISIBLE

            textDumbbells.visibility = View.VISIBLE
            textDumbbells.text = getString(R.string.dumbbells_human, dogTracker.dumbbellLocations.size)

            if(dogTracker.isPaused) {
                buttonPause.visibility = View.GONE
                buttonResume.visibility = View.VISIBLE
            }
            else {
                buttonPause.visibility = View.VISIBLE
                buttonResume.visibility = View.GONE
            }
        }
        else if(dogTracker.state == DogTracker.State.START_DOG) {
            viewStartDog.visibility = View.VISIBLE
            textDistance.visibility = View.VISIBLE
            textTime.visibility = View.VISIBLE

            textDumbbells.visibility = View.VISIBLE
            textDumbbells.text = getString(R.string.dumbbells_human, dogTracker.dumbbellLocations.size)
        }
        else {
            viewDog.visibility = View.VISIBLE
            textDistance.visibility = View.VISIBLE
            textTime.visibility = View.VISIBLE

            textDumbbells.visibility = View.VISIBLE
            textDumbbells.text = getString(R.string.dumbbells_dog, dogTracker.dumbbellsFound, dogTracker.dumbbellLocations.size)

            if(dogTracker.dumbbellsFound + dogTracker.dumbbellsMissed >= dogTracker.dumbbellLocations.size)
                groupDumbbell.visibility = View.GONE
            else
                groupDumbbell.visibility = View.VISIBLE
        }

        updateTrackingStatisticsUI()
    }

    // update tracking statistics UI, i.e. distance and time
    private fun updateTrackingStatisticsUI() {
        textDistance.text = getString(R.string.distance, dogTracker.distance)
        val minutes = dogTracker.time / 60000
        val seconds = (dogTracker.time % 60000) / 1000
        textTime.text = getString(R.string.time, minutes, seconds)
    }

    private fun navigationListener(menuItem: MenuItem): Boolean {
        when(menuItem.itemId) {
            R.id.side_navigation_tracks -> startActivity(Intent(this, TracksActivity::class.java))
            R.id.side_navigation_dogs -> startActivity(Intent(this, ProfilesActivity::class.java))
            //R.id.side_navigation_settings -> startActivity(Intent(this, SettingsActivity::class.java))
            R.id.side_navigation_logout -> {
                auth.signOut()
                startActivity(Intent(this, LoginActivity::class.java))
            }
        }
        return true
    }

    private fun loadCurrentUser() {
        DatabaseIO.getCurrentUser { user ->
            currentUser = user
            Log.e("MainActivity", currentUser.name)

            DatabaseIO.retrieveTracks { tracks ->
                for (track in tracks) {
                    currentUser.addTrack(track)
                    Log.e("MainActivity", track.state.toString())
                }
            }

            DatabaseIO.retrieveDogs { dogs ->
                if (currentUser.dogs != null) {
                    currentUser.dogs = dogs
                    if (currentUser.dogs != null) {
                        Log.e("MainActivity", currentUser.dogs.dogs.size.toString())
                    }
                }
            }

            DatabaseIO.retrieveSelectedDog { dog ->
                if (currentUser.dogs != null) {
                    currentUser.selectedDog = dog
                    if (currentUser.selectedDog != null) {
                        Log.e("MainActivity", "Selected dog: " + currentUser.selectedDog.name)
                        updateSelectedDog()
                    }
                }
            }
        }
    }

    private fun updateSelectedDog() {
        if(!::currentUser.isInitialized) return
        if(currentUser.selectedDog == null) return

        findViewById<TextView>(R.id.text_selected_name).text = currentUser.selectedDog.name

        if (currentUser.selectedDog.bitmapPicture != null) {
            val roundDrawable: RoundedBitmapDrawable = RoundedBitmapDrawableFactory.create(
                resources,
                currentUser.selectedDog.bitmapPicture
            )
            roundDrawable.isCircular = true
            findViewById<AppCompatImageView>(R.id.image_selected_profile_pic).setImageDrawable(roundDrawable)
        }
        else
            findViewById<AppCompatImageView>(R.id.image_selected_profile_pic).setImageResource(R.drawable.empty_profile_picture)
    }
}



