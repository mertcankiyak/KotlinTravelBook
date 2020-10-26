package com.kodless.kotlintravelbook.Model

import java.io.Serializable

//Serializable yapmamızın sebebi activityler arasında veri aktarımını sağlamak için.

class Place(var adress: String?, var latitude: Double?, var longitude: Double?) :Serializable {



}