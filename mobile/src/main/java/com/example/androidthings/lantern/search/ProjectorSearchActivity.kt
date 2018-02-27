package com.example.androidthings.lantern.search

import android.app.Activity
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.design.widget.Snackbar.LENGTH_INDEFINITE
import android.support.design.widget.Snackbar.LENGTH_LONG
import android.util.Log
import com.example.androidthings.lantern.App
import com.example.androidthings.lantern.home.HomeActivity
import com.example.androidthings.lantern.R
import com.example.androidthings.lantern.configuration.ProjectorClient
import com.example.androidthings.lantern.configuration.ConnectionState
import com.example.androidthings.lantern.configuration.DiscoveryState
import com.example.androidthings.lantern.configuration.Endpoint
import kotlinx.android.synthetic.main.activity_projector_search.*
import java.util.*

class ProjectorSearchActivity : AppCompatActivity(),
        ProjectorListFragment.OnProjectorSelectedListener,
        ProjectorClient.ProjectorClientFailureListener {

    companion object {
        private val TAG: String = ProjectorSearchActivity::class.java.simpleName
        const val HOME_ACTIVITY_REQUEST = 1
    }

    private val clientObserver: Observer = Observer { _, _ -> onClientUpdated() }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_projector_search)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationIcon(R.drawable.back_chevron)
        toolbar.setNavigationOnClickListener {
            //TODO - restart nearby discovery and clear endpoints?
        }
    }

    override fun onResume() {
        super.onResume()
        App.instance.client.addObserver(clientObserver)
        App.instance.client.failureListener = this
        if (App.instance.client.discoveryState == DiscoveryState.UNINITIALISED) {
            App.instance.client.startDiscovery()
        }
        update()
    }

    override fun onPause() {
        super.onPause()
        App.instance.client.deleteObserver(clientObserver)
        App.instance.client.failureListener = null
    }

    private fun onClientUpdated() {
        update()
    }

    override fun onStartDiscoveryFailure() {
        val snackBar = Snackbar.make(fragment_container, "Failed to start Nearby Connections", LENGTH_INDEFINITE)
        snackBar.setAction("Try again", { App.instance.client.startDiscovery() })
        snackBar.show()
    }

    override fun onRequestConnectionFailure() {
        val snackBar = Snackbar.make(fragment_container, "Failed to connect to projector", LENGTH_LONG)
        snackBar.show()
    }

    override fun onProjectorSelected(endpoint: Endpoint) {
        App.instance.client.connectTo(endpoint.id)
        showProjectorConnectingFragment(endpoint.info.endpointName)
    }

    private fun update() {
        when (App.instance.client.connectionState) {
            ConnectionState.CONNECTED -> {
                showHomeActivity()
                return
            }
        }
        when (App.instance.client.discoveryState) {
            DiscoveryState.LOOKING_FOR_ENDPOINTS -> {
                showProjectorSearchFragment()
                return
            }
            DiscoveryState.ENDPOINTS_AVAILABLE -> {
                showProjectorListFragment()
                return
            }
        }
    }

    private fun showProjectorSearchFragment() {
        supportActionBar?.hide()
        val searchFragment = ProjectorSearchFragment()
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.fragment_container, searchFragment)
        fragmentTransaction.commit()
    }

    private fun showProjectorListFragment() {
        supportActionBar?.show()
        val listFragment = ProjectorListFragment()
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.fragment_container, listFragment)
        fragmentTransaction.commit()
    }

    private fun showProjectorConnectingFragment(name: String) {
        supportActionBar?.show()
        val connectingFragment = ProjectorConnectingFragment()
        connectingFragment.arguments = Bundle().apply { putString(ProjectorConnectingFragment.ARG_NAME, name) }
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.fragment_container, connectingFragment)
        fragmentTransaction.commit()
    }

    private fun showHomeActivity() {
        val intent = Intent(this, HomeActivity::class.java)
        startActivityForResult(intent, HOME_ACTIVITY_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == HOME_ACTIVITY_REQUEST) {
            when (resultCode) {
                HomeActivity.RESULT_DISCONNECTED -> {
                    val snackBar = Snackbar.make(fragment_container, "Lost connection to projector", LENGTH_LONG)
                    snackBar.show()
                    // TODO - restart discovery?
                }
                Activity.RESULT_CANCELED -> {
                    App.instance.client.disconnect()
                }
            }
        }
    }
}