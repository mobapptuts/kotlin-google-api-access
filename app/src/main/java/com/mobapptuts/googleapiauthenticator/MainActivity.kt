package com.mobapptuts.googleapiauthenticator

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusButton.setOnClickListener {
            checkPreConditions()
        }
    }

    private val googleApiAvailability by lazy {
        GoogleApiAvailability.getInstance()
    }

    private val googleConnectionStatus by lazy {
        googleApiAvailability.isGooglePlayServicesAvailable(this)
    }

    companion object {
        val REQUEST_GOOGLE_PLAY_SERVICES = 1000
    }

    private fun isGooglePlayServicesAvailable() =
            googleConnectionStatus == ConnectionResult.SUCCESS

    private fun showGPSErrorDialog() {
        val dialog = googleApiAvailability.getErrorDialog(
                this, googleConnectionStatus, REQUEST_GOOGLE_PLAY_SERVICES
        )
        dialog.show()
    }

    private fun acquireGooglePlayServices() {
        if (googleApiAvailability.isUserResolvableError(googleConnectionStatus))
            showGPSErrorDialog()
        else
            statusButton.text = resources.getString(R.string.gps_failed)
    }

    private fun checkPreConditions() {
        if (!isGooglePlayServicesAvailable())
            acquireGooglePlayServices()
        else if (!isNeworkAvailable())
            statusButton.text = resources.getString(R.string.network_not_available)
        else
            statusButton.text = resources.getString(R.string.gps_network_available)
    }

    private fun isNeworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE)
        return if (connectivityManager is ConnectivityManager) {
            val networkInfo = connectivityManager.activeNetworkInfo
            networkInfo.isConnected
        } else false
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_GOOGLE_PLAY_SERVICES -> {
                if(resultCode != Activity.RESULT_OK) {
                    Toast.makeText(this, "Please install Google Play Services", Toast.LENGTH_SHORT).show()
                } else
                    checkPreConditions()
            }
            else -> {
                throw IllegalArgumentException("Unrecongnized request code $requestCode")
            }
        }
    }
}
