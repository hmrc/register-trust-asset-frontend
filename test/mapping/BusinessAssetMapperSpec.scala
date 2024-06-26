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

package mapping

import base.SpecBase
import generators.Generators
import models.Status.Completed
import models.{AddressType, BusinessAssetType, InternationalAddress, UKAddress, WhatKindOfAsset}
import org.scalatest.matchers.must.Matchers
import org.scalatest.OptionValues
import pages.AssetStatus
import pages.asset.WhatKindOfAssetPage
import pages.asset.business._

class BusinessAssetMapperSpec extends SpecBase with Matchers with OptionValues with Generators {

  private val businessAssetMapper: BusinessAssetMapper = injector.instanceOf[BusinessAssetMapper]

  private val assetValue: Long = 123L

  "BusinessAssetMapper" must {

    "must not be able to create a business asset when no description or value in user answers" in {

      val userAnswers =
        emptyUserAnswers
          .set(WhatKindOfAssetPage(0), WhatKindOfAsset.Business)
          .success
          .value

      businessAssetMapper.build(userAnswers) mustNot be(defined)
    }

    "must able to create a Business Asset" when {

      "UK address" in {

        val userAnswers =
          emptyUserAnswers
            .set(WhatKindOfAssetPage(0), WhatKindOfAsset.Business)
            .success
            .value
            .set(BusinessNamePage(0), "Test")
            .success
            .value
            .set(BusinessDescriptionPage(0), "Description")
            .success
            .value
            .set(BusinessAddressUkYesNoPage(0), true)
            .success
            .value
            .set(
              BusinessUkAddressPage(0),
              UKAddress("26", "Grangetown", Some("Tyne and Wear"), Some("Newcastle"), "Z99 2YY")
            )
            .success
            .value
            .set(BusinessValuePage(0), assetValue)
            .success
            .value
            .set(AssetStatus(0), Completed)
            .success
            .value

        businessAssetMapper.build(userAnswers).value mustBe List(
          BusinessAssetType(
            "Test",
            "Description",
            AddressType("26", "Grangetown", Some("Tyne and Wear"), Some("Newcastle"), Some("Z99 2YY"), "GB"),
            assetValue
          )
        )
      }

      "when international address" in {

        val userAnswers =
          emptyUserAnswers
            .set(WhatKindOfAssetPage(0), WhatKindOfAsset.Business)
            .success
            .value
            .set(BusinessNamePage(0), "Test")
            .success
            .value
            .set(BusinessDescriptionPage(0), "Description")
            .success
            .value
            .set(BusinessAddressUkYesNoPage(0), false)
            .success
            .value
            .set(BusinessInternationalAddressPage(0), InternationalAddress("1", "Broadway", Some("New York"), "US"))
            .success
            .value
            .set(BusinessValuePage(0), assetValue)
            .success
            .value
            .set(AssetStatus(0), Completed)
            .success
            .value

        businessAssetMapper.build(userAnswers).value mustBe List(
          BusinessAssetType(
            "Test",
            "Description",
            AddressType("1", "Broadway", Some("New York"), None, None, "US"),
            assetValue
          )
        )
      }
    }

    "must able to create multiple Business Assets" in {

      val userAnswers =
        emptyUserAnswers
          .set(WhatKindOfAssetPage(0), WhatKindOfAsset.Business)
          .success
          .value
          .set(BusinessNamePage(0), "Test 1")
          .success
          .value
          .set(BusinessDescriptionPage(0), "Description 1")
          .success
          .value
          .set(BusinessAddressUkYesNoPage(0), true)
          .success
          .value
          .set(
            BusinessUkAddressPage(0),
            UKAddress("26", "Grangetown", Some("Tyne and Wear"), Some("Newcastle"), "Z99 2YY")
          )
          .success
          .value
          .set(BusinessValuePage(0), assetValue)
          .success
          .value
          .set(AssetStatus(0), Completed)
          .success
          .value
          .set(WhatKindOfAssetPage(1), WhatKindOfAsset.Business)
          .success
          .value
          .set(BusinessNamePage(1), "Test 2")
          .success
          .value
          .set(BusinessDescriptionPage(1), "Description 2")
          .success
          .value
          .set(BusinessAddressUkYesNoPage(1), true)
          .success
          .value
          .set(
            BusinessUkAddressPage(1),
            UKAddress("26", "Grangetown", Some("Tyne and Wear"), Some("Newcastle"), "Z99 2YY")
          )
          .success
          .value
          .set(BusinessValuePage(1), assetValue)
          .success
          .value
          .set(AssetStatus(1), Completed)
          .success
          .value

      businessAssetMapper.build(userAnswers).value mustBe List(
        BusinessAssetType(
          "Test 1",
          "Description 1",
          AddressType("26", "Grangetown", Some("Tyne and Wear"), Some("Newcastle"), Some("Z99 2YY"), "GB"),
          assetValue
        ),
        BusinessAssetType(
          "Test 2",
          "Description 2",
          AddressType("26", "Grangetown", Some("Tyne and Wear"), Some("Newcastle"), Some("Z99 2YY"), "GB"),
          assetValue
        )
      )

      businessAssetMapper.build(userAnswers).value.length mustBe 2
    }
  }
}
