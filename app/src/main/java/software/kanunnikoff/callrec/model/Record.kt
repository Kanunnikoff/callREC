package software.kanunnikoff.callrec.model

/**
 * Created by dmitry on 17/10/2017.
 */
data class Record(
        var id: Long = -1L,
        var title: String = "title'",
        var outputFormat: String = "MPEG4",
        var outputFile: String = "file.mp3",
        var audioEncoder: String = "ACC",
        var audioEncodingBitRate: String = "16000",
        var audioSamplingRate: String = "96000",
        var audioChannels: String = "MONO",
        var isFavored: Int = 0,
        var fileSize: String = "1kb",
        var duration: String = "1s",
        var date: String = "today",
        var thumbnail: ByteArray = ByteArray(1)
) {
    override fun equals(other: Any?): Boolean {
        return if (other !is Record) {
            false
        } else other.id == id
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + outputFormat.hashCode()
        result = 31 * result + outputFile.hashCode()
        result = 31 * result + audioEncoder.hashCode()
        result = 31 * result + audioEncodingBitRate.hashCode()
        result = 31 * result + audioSamplingRate.hashCode()
        result = 31 * result + audioChannels.hashCode()
        result = 31 * result + isFavored
        result = 31 * result + fileSize.hashCode()
        result = 31 * result + duration.hashCode()
        result = 31 * result + date.hashCode()
        result = 31 * result + thumbnail.hashCode()
        return result
    }
}