package geographic.boger.me.nationalgeographic.main.selectdate

import geographic.boger.me.nationalgeographic.core.NGRumtime

/**
 * Created by BogerChan on 2017/6/28.
 */
data class SelectDateData(val total: String, var page: String, var pagecount: String,
                          var album: MutableList<SelectDateAlbumData>) {
}

data class SelectDateAlbumData(var id: String, var title: String, var url: String,
                               var addtime: String, var adshow: String, var fabu: String,
                               var encoded: String, var amd5: String, var sort: String,
                               var ds: String, var timing: String, var timingpublish: String) {

    @Transient
    private var locale: SelectDateAlbumData? = null

    //can't use lazy init, not thread safe
    fun locale(): SelectDateAlbumData {
        if (locale == null) {
            locale = copy(title = NGRumtime.locale(title))
        }
        return locale!!
    }
}