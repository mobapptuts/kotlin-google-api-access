package com.mobapptuts.googleapiauthenticator

import android.Manifest
import android.accounts.AccountManager
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.util.ExponentialBackOff
import com.google.api.services.youtube.YouTubeScopes
import kotlinx.android.synthetic.main.activity_main.*
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusButton.setOnClickListener {
            checkPreConditions()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private val googleApiAvailability by lazy {
        GoogleApiAvailability.getInstance()
    }

    private val googleConnectionStatus by lazy {
        googleApiAvailability.isGooglePlayServicesAvailable(this)
    }

    @AfterPermissionGranted(Companion.REQUEST_PERMISSION_GET_ACCOUNTS)
    fun chooseGoogleAccount() {
        if (EasyPermissions.hasPermissions(this, Manifest.permission.GET_ACCOUNTS)) {
            val accountName = getPreferences(Context.MODE_PRIVATE).getString(PREF_ACCOUNT_NAME, null)
            if (accountName != null) {
                googleAccountCredential.setSelectedAccountName(accountName)
                checkPreConditions()
            } else
                startActivityForResult(googleAccountCredential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER)
        } else
            EasyPermissions.requestPermissions(this,
                    "This app requires to access your Google Account via contacts",
                    REQUEST_PERMISSION_GET_ACCOUNTS, Manifest.permission.GET_ACCOUNTS)
    }

    companion object {
        val REQUEST_GOOGLE_PLAY_SERVICES = 1000
        const val REQUEST_PERMISSION_GET_ACCOUNTS = 1001
        val REQUEST_ACCOUNT_PICKER = 1002
        val REQUEST_ACCOUNT_AUTHORIZATION = 1003
        val SCOPES: MutableList<String> = mutableListOf(YouTubeScopes.YOUTUBE_READONLY)
        val PREF_ACCOUNT_NAME = "accountName"
    }

    private val googleAccountCredential by lazy {
        GoogleAccountCredential.usingOAuth2(this, SCOPES)
                .setBackOff(ExponentialBackOff())
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
        else if (googleAccountCredential.selectedAccountName == null)
            chooseGoogleAccount()
        else
            statusButton.text = resources.getString(R.string.gps_network_accountname_available)
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
            REQUEST_ACCOUNT_PICKER -> {
                if (resultCode == Activity.RESULT_OK && data != null && data.extras != null) {
                    val accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
                    if (accountName != null) {
                        val settings = getPreferences(Context.MODE_PRIVATE)
                        val editor = settings.edit()
                        editor.putString(PREF_ACCOUNT_NAME, accountName)
                        editor.apply()
                        googleAccountCredential.setSelectedAccountName(accountName)
                        checkPreConditions()
                    }
                }
            }
            else -> {
                throw IllegalArgumentException("Unrecongnized request code $requestCode")
            }
        }
    }
}
