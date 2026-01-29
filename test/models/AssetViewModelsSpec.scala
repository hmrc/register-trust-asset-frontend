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

import base.SpecBase
import models.Constants._
import models.Status.InProgress
import models.WhatKindOfAsset._
import viewmodels._

class AssetViewModelsSpec extends SpecBase {

  val money: MoneyAssetViewModel                   = MoneyAssetViewModel(Money, None, InProgress)

  val propertyOrLand: PropertyOrLandAssetViewModel =
    PropertyOrLandAssetViewModel(PropertyOrLand, None, None, None, InProgress)

  val share: ShareAssetViewModel                   = ShareAssetViewModel(PropertyOrLand, None, InProgress)
  val business: BusinessAssetViewModel             = BusinessAssetViewModel(PropertyOrLand, None, InProgress)
  val partnership: PartnershipAssetViewModel       = PartnershipAssetViewModel(PropertyOrLand, None, InProgress)
  val other: OtherAssetViewModel                   = OtherAssetViewModel(PropertyOrLand, None, InProgress)
  val nonEeaBusiness: NonEeaBusinessAssetViewModel = NonEeaBusinessAssetViewModel(PropertyOrLand, None, InProgress)

  "AssetViewModels" when {

    "nonMaxedOutOptions" when {

      "return asset types that aren't maxed out" when {

        "taxable" when {

          "not maxed out" in {

            val assets = AssetViewModels(
              monetary = Some(Nil),
              propertyOrLand = Some(Nil),
              shares = Some(Nil),
              business = Some(Nil),
              partnerShip = Some(Nil),
              other = Some(Nil),
              nonEEABusiness = Some(Nil)
            )

            assets.nonMaxedOutOptions.size mustEqual 7
          }

          "maxed out" in {

            val assets = AssetViewModels(
              monetary = Some(List.fill(MAX_MONEY_ASSETS)(money)),
              propertyOrLand = Some(List.fill(MAX_PROPERTY_OR_LAND_ASSETS)(propertyOrLand)),
              shares = Some(List.fill(MAX_SHARES_ASSETS)(share)),
              business = Some(List.fill(MAX_BUSINESS_ASSETS)(business)),
              partnerShip = Some(List.fill(MAX_PARTNERSHIP_ASSETS)(partnership)),
              other = Some(List.fill(MAX_OTHER_ASSETS)(other)),
              nonEEABusiness = Some(List.fill(MAX_NON_EEA_BUSINESS_ASSETS)(nonEeaBusiness))
            )

            assets.nonMaxedOutOptions.size mustEqual 0
          }
        }

        "non-taxable" when {

          "not maxed out" in {

            val assets = AssetViewModels(
              monetary = None,
              propertyOrLand = None,
              shares = None,
              business = None,
              partnerShip = None,
              other = None,
              nonEEABusiness = Some(Nil)
            )

            assets.nonMaxedOutOptions.size mustEqual 1
          }

          "maxed out" in {

            val assets = AssetViewModels(
              monetary = None,
              propertyOrLand = None,
              shares = None,
              business = None,
              partnerShip = None,
              other = None,
              nonEEABusiness = Some(List.fill(MAX_NON_EEA_BUSINESS_ASSETS)(nonEeaBusiness))
            )

            assets.nonMaxedOutOptions.size mustEqual 0
          }
        }

      }
    }
  }

}
