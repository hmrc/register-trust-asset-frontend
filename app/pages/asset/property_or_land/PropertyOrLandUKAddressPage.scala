/*
 * Copyright 2022 HM Revenue & Customs
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

package pages.asset.property_or_land

import models.UKAddress
import pages.QuestionPage
import pages.asset.property_or_land.PropertyOrLandUKAddressPage.key
import play.api.libs.json.JsPath
import sections.Assets

final case class PropertyOrLandUKAddressPage(index: Int) extends QuestionPage[UKAddress] {

  override def path: JsPath = Assets.path \ index \ toString

  override def toString: String = key
}

object PropertyOrLandUKAddressPage {
  val key: String = "propertyOrLandUkAddress"
}
