package me.gegenbauer.anim.canvas

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.gegenbauer.anim.R

class AnimTestActivity : AppCompatActivity() {

    private val navigationView: CarNavigationView by lazy {
        findViewById(R.id.car_navigation_view)!!
    }

    private val searchState by lazy { SearchState(navigationView) }
    private val nearbyState by lazy { NearbyState(navigationView) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_anim_test)
        navigationView.state = searchState
        var progress = 8000
        lifecycleScope.launch {
            repeat(Int.MAX_VALUE) {
                searchState.progress = progress / 10000f
                if (progress == 10000) {
                    delay(5000)
                    navigationView.state = nearbyState
                }
                progress += 3
                if (progress > 10000) {
                    progress = 10000
                }
                delay(3)
            }
        }
    }
}