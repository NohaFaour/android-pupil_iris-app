package com.example.backend_trial

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import android.widget.Button
import com.google.android.material.tabs.TabLayout

class HomeFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // Set up Capture button
        view.findViewById<Button>(R.id.btnCapture).setOnClickListener {
            activity?.findViewById<TabLayout>(R.id.tab_layout)?.getTabAt(1)?.select()
        }

        // Set up Gallery button
        view.findViewById<Button>(R.id.btnGallery).setOnClickListener {
            activity?.findViewById<TabLayout>(R.id.tab_layout)?.getTabAt(2)?.select()
        }

        return view
    }
} 