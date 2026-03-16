package com.suprajit.uvcluster

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.navigation.fragment.findNavController

class ThermalRunawayFragment : Fragment() {

    private lateinit var tvPark: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_thermal_runaway, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        initClickListener()
    }

    private fun initViews(view: View) {
        tvPark = view.findViewById(R.id.tvPark)
    }

    private fun initClickListener() {
        tvPark.setOnClickListener {
            findNavController().navigate(R.id.dashboardFragment)
        }
    }
}

