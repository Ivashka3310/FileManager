package com.example.shared.models

data class DiskInfo(
    val mountPoint: String,
    val device: String,
    val fsType: String,
    val fsName: String,
    val totalSpace: Long,
    val usableSpace: Long,
    val unallocatedSpace: Long,
    val isReadOnly: Boolean,
    val options: List<String>
)
