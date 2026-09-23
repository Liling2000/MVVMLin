package com.jiyi.power.app.bean

import androidx.annotation.StringRes
import com.jiyi.power.R

enum class Certification(@StringRes val titleRes: Int, @StringRes val descriptionRes: Int) {
    IATA(
        R.string.certification_iata_title,
        R.string.certification_iata_description
    ),
    ROHS(
        R.string.certification_rohs_title,
        R.string.certification_rohs_description
    ),
    WEEE(
        R.string.certification_weee_title,
        R.string.certification_weee_description
    ),
    IEC(
        R.string.certification_iec_title,
        R.string.certification_iec_description
    ),
    RECYCLING(
        R.string.certification_recycling_title,
        R.string.certification_recycling_description
    ),
    CCC(R.string.certification_ccc_title, R.string.certification_ccc_description),
}
