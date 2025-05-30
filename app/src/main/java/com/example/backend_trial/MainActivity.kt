package com.example.backend_trial

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayout

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = getString(R.string.app_name)

        val tabLayout: TabLayout = findViewById(R.id.tab_layout)
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        openFragment(HomeFragment())
                        toolbar.title = getString(R.string.app_name)
                    }
                    1 -> {
                        openFragment(CameraFragment())
                        toolbar.title = "Camera"
                    }
                    2 -> {
                        openFragment(GalleryFragment())
                        toolbar.title = "Gallery"
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // Set default fragment
        if (savedInstanceState == null) {
            openFragment(HomeFragment())
        }
    }

    private fun openFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, fragment)
            .commit()
    }
}