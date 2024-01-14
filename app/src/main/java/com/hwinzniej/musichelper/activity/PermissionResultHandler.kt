package com.hwinzniej.musichelper.activity

interface PermissionResultHandler {
    fun onPermissionResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    )
}