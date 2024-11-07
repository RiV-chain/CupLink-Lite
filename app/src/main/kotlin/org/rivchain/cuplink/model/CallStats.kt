package org.rivchain.cuplink.model

data class CallStats(
    var videoCodec: String? = null,
    var audioCodec: String? = null,
    var inputWidth: Int = 0,
    var inputHeight: Int = 0,
    var inputFrameRate: Int = 0,
    var outputWidth: Int = 0,
    var outputHeight: Int = 0,
    var outputFrameRate: Int = 0,
    var receivedVideoBitrateKbps: Double = 0.0,
    var sentVideoBitrateKbps: Double = 0.0,
    var receivedAudioBitrateKbps: Double = 0.0,
    var sentAudioBitrateKbps: Double = 0.0
)