/*
 * Copyright 2020 HM Revenue & Customs
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

package pages.asset.money

import controllers.asset.money.routes._
import models.NormalMode
import pages.QuestionPage
import play.api.libs.json.JsPath
import play.api.mvc.Call
import sections.Assets

final case class AssetMoneyValuePage(index: Int) extends QuestionPage[String] {

  override def path: JsPath = JsPath \ Assets \ index \ toString

  override def toString: String = "assetMoneyValue"

  override def route(draftId: String): Call =
    AssetMoneyValueController.onPageLoad(NormalMode, index, draftId)
}
