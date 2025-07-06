package ir.zarbang.FFTsounds.fft

/**
 * A simple data class to hold the calculated values for each frequency band.
 */
data class FrequencyBands(
    val bass: Float = 0f,
    val mid: Float = 0f,
    val treble: Float = 0f
)
