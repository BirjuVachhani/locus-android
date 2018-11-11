package com.birjuvachhani.locationextension

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.support.v4.app.Fragment

/**
 * Created by Birju Vachhani on 09-11-2018.
 */
class LocationHelper : Fragment() {
    var isOneTime = false
    private var isRationaleDisplayed = false
    private var isJustBlocked = true

    companion object {
        const val TAG = "LocationHelper"
        fun newInstance(): LocationHelper {
            return LocationHelper()
        }
    }

    var success: (Location) -> Unit = {}
    var failure: (isDenied: Boolean, t: Throwable?) -> Unit = { _, _ -> }

    private val LOCATION_PERMISSION = Manifest.permission.ACCESS_FINE_LOCATION
    private val REQUEST_LOCATION_CODE = 7

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
    }

    fun startLocationProcess(
        success: (Location) -> Unit,
        failure: (isDenied: Boolean, t: Throwable?) -> Unit,
        isOneTime: Boolean
    ) {
        this.success = success
        this.failure = failure
        this.isOneTime = isOneTime
        initPermissionModel()
    }

    private fun initPermissionModel() {
        when (hasLocationPermission()) {
            //has permission to access location
            true -> initPermissionModel()
            false -> {
                //doesn't have permission, checking if user has been asked for permission earlier
                when (isFirstRequest()) {
                    //permission is requested first time, directly prompt user for permission
                    true -> requestLocationPermission()
                    false -> {
                        //permission is not asked for first time, display rationale and then prompt user for permission
                        displayRationale()
                        isRationaleDisplayed = true
                    }
                }
            }
        }
    }

    private fun displayRationale() {
        val alertDialog = AlertDialog.Builder(requireContext())
            .setTitle("Location Permission Required!")
            .setMessage("Location permission is required to perform this action")
            .setPositiveButton("GRANT") { dialog, _ ->
                requestLocationPermission()
                dialog.dismiss()
            }
            .setNegativeButton("CANCEL") { dialog, _ ->
                dialog.dismiss()
            }.create().show()
    }

    private fun requestLocationPermission() {
        requestPermissions(arrayOf(LOCATION_PERMISSION), REQUEST_LOCATION_CODE)
    }

    private fun isFirstRequest(): Boolean {
        return if (isApiLevelAbove23())
            !requireActivity().shouldShowRequestPermissionRationale(LOCATION_PERMISSION)
        else
            false
    }

    private fun hasLocationPermission(): Boolean {
        return if (isApiLevelAbove23())
            requireActivity().checkSelfPermission(LOCATION_PERMISSION) == PackageManager.PERMISSION_GRANTED
        else
            true
    }

    private fun isApiLevelAbove23(): Boolean {
        return Build.VERSION.SDK_INT >= 23
    }

    private fun initLocationTracking() {
        //init location here
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_LOCATION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    initLocationTracking()
                } else {
                    if (!shouldShowRequestPermissionRationale(LOCATION_PERMISSION)) {
                        //means permission is permanently blocked by user
                        if (!isJustBlocked) {
                            showPermissionBlockedDialog()
                        } else
                            isJustBlocked = false
                    }
                }
            }
        }
    }

    private fun showPermissionBlockedDialog() {
        val alertDialog = AlertDialog.Builder(requireContext())
            .setTitle("Location Permission Blocked")
            .setMessage("Location permission is blocked. Please enable it settings.")
            .setPositiveButton("ENABLE") { dialog, _ ->
                dialog.dismiss()
                openSettings()
            }
            .setNegativeButton("CANCEL") { dialog, _ ->
                dialog.dismiss()
            }.create().show()
    }

    private fun openSettings() {
        val intent = Intent()
        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        val uri = Uri.fromParts("package", requireActivity().packageName, null)
        intent.data = uri
        startActivity(intent)
    }
}