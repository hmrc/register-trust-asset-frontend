/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package mapping.reads

import models.WhatKindOfAsset.Business
import models.{Address, WhatKindOfAsset}
import pages.asset.WhatKindOfAssetPage
import pages.asset.business._
import play.api.libs.functional.syntax._
import play.api.libs.json.{Reads, __}

final case class BusinessAsset(
  override val whatKindOfAsset: WhatKindOfAsset,
  assetName: String,
  assetDescription: String,
  address: Address,
  currentValue: Long
) extends Asset {

  override val arg: String = assetName
}

object BusinessAsset {

  implicit lazy val reads: Reads[BusinessAsset] = {

    val addressReads: Reads[Address] = {
      (__ \ BusinessUkAddressPage.key).read[Address] orElse
        (__ \ BusinessInternationalAddressPage.key).read[Address]
    }

    ((__ \ WhatKindOfAssetPage.key).read[WhatKindOfAsset].filter(_ == Business) and
      (__ \ BusinessNamePage.key).read[String] and
      (__ \ BusinessDescriptionPage.key).read[String] and
      addressReads and
      (__ \ BusinessValuePage.key).read[Long])(BusinessAsset.apply _)

  }
}
