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

package models

import models.WhatKindOfAsset.prefix
import play.api.i18n.Messages
import viewmodels._

sealed trait WhatKindOfAsset {
  implicit class AssetLabel(asset: WhatKindOfAsset) {
    def label(implicit messages: Messages): String = messages(s"$prefix.$asset")
  }
}

object WhatKindOfAsset extends Enumerable.Implicits {

  case object Money extends WithName("Money") with WhatKindOfAsset
  case object PropertyOrLand extends WithName("PropertyOrLand") with WhatKindOfAsset
  case object Shares extends WithName("Shares") with WhatKindOfAsset
  case object Business extends WithName("Business") with WhatKindOfAsset
  case object Partnership extends WithName("Partnership") with WhatKindOfAsset
  case object Other extends WithName("Other") with WhatKindOfAsset
  case object NonEeaBusiness extends WithName("NonEeaBusiness") with WhatKindOfAsset

  val values: List[WhatKindOfAsset] = List(
    Money,
    PropertyOrLand,
    Shares,
    Business,
    NonEeaBusiness,
    Partnership,
    Other
  )

  val prefix: String = "whatKindOfAsset"

  def options(kindsOfAsset: List[WhatKindOfAsset] = values): List[RadioOption] = kindsOfAsset.map { value =>
    RadioOption(prefix, value.toString)
  }

  implicit val enumerable: Enumerable[WhatKindOfAsset] =
    Enumerable(values.map(v => v.toString -> v): _*)

  def nonMaxedOutOptions(assets: AssetViewModels, assetTypeAtIndex: Option[WhatKindOfAsset]): List[RadioOption] = {

    def meetsLimitConditions(assetSize: AssetSize): Boolean =
      assetSize.size < assetSize.maxSize || assetTypeAtIndex.contains(assetSize.kindOfAsset)

    options(assets.assetSizes.filter(meetsLimitConditions).map(_.kindOfAsset))
  }
}
