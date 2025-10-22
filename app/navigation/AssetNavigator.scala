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

package navigation

import config.FrontendAppConfig
import controllers.asset.routes.AssetInterruptPageController
import controllers.asset.noneeabusiness.routes.NameController
import controllers.routes.SessionExpiredController
import models.Constants._
import models.WhatKindOfAsset._
import models.{AddAssets, UserAnswers, WhatKindOfAsset}
import pages.Page
import pages.asset._
import play.api.mvc.Call
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HttpVerbs.GET
import viewmodels._

import javax.inject.Inject

class AssetNavigator @Inject() (config: FrontendAppConfig) extends Navigator {

  override def route(draftId: String): PartialFunction[Page, AffinityGroup => UserAnswers => Call] = {
    case TrustOwnsNonEeaBusinessYesNoPage =>
      _ =>
        ua =>
          yesNoNav(
            ua = ua,
            fromPage = TrustOwnsNonEeaBusinessYesNoPage,
            yesCall = AssetInterruptPageController.onPageLoad(draftId),
            noCall = assetsCompletedRoute(draftId)
          )
    case AssetInterruptPage               => _ => ua => redirectToAddToPageIfTaxable(ua, draftId)
    case WhatKindOfAssetPage(index)       => _ => ua => whatKindOfAssetRoute(ua, index, draftId)
    case AddAssetsPage                    => _ => addAssetsRoute(draftId)
    case AddAnAssetYesNoPage              =>
      _ =>
        ua =>
          yesNoNav(
            ua = ua,
            fromPage = AddAnAssetYesNoPage,
            yesCall = redirectToAddToPageIfTaxable(ua, draftId),
            noCall = assetsCompletedRoute(draftId)
          )
  }

  private def redirectToAddToPageIfTaxable(userAnswers: UserAnswers, draftId: String): Call =
    AssetNavigator.routeToIndex(
      answers = userAnswers,
      route = if (userAnswers.isTaxable) {
        controllers.asset.routes.WhatKindOfAssetController.onPageLoad
      } else {
        controllers.asset.noneeabusiness.routes.NameController.onPageLoad
      },
      draftId = draftId
    )

  private def assetsCompletedRoute(draftId: String): Call =
    Call(GET, config.registrationProgressUrl(draftId))

  private def addAssetsRoute(draftId: String)(answers: UserAnswers): Call = {
    answers.get(AddAssetsPage) match {
      case Some(AddAssets.YesNow) if nonTaxableNonEeaBusinessRoute(draftId).isDefinedAt(answers) => nonTaxableNonEeaBusinessRoute(draftId)(answers)
      case Some(AddAssets.YesNow) => AssetNavigator.addAssetRoute(answers, draftId)
      case Some(_)                => assetsCompletedRoute(draftId)
      case _                      => SessionExpiredController.onPageLoad
    }
  }

  private def nonTaxableNonEeaBusinessRoute(draftId: String): PartialFunction[UserAnswers, Call] = {
    case answers if !answers.isTaxable && answers.assets.nonEEABusiness.exists(_.nonEmpty) => {
      val numNonEeaAssets = answers.assets.nonEEABusiness.get.size
      val index = numNonEeaAssets - 1

      NameController.onPageLoad(index, draftId)
    }
  }

  private def whatKindOfAssetRoute(answers: UserAnswers, index: Int, draftId: String): Call =
    answers.get(WhatKindOfAssetPage(index)) match {
      case Some(kindOfAsset) if kindOfAsset == NonEeaBusiness =>
        controllers.asset.noneeabusiness.routes.NonEeaInterruptController.onPageLoad(index, draftId)
      case Some(kindOfAsset: WhatKindOfAsset)                 =>
        AssetNavigator.addAssetNowRoute(kindOfAsset, answers, draftId, Some(index))
      case _                                                  => SessionExpiredController.onPageLoad
    }

}

object AssetNavigator {

  def addAssetRoute(answers: UserAnswers, draftId: String): Call = {

    case class AssetRoute(size: Int, maxSize: Int, route: Call)

    val routes: List[AssetRoute] = List(
      (answers.assets.monetary, MAX_MONEY_ASSETS, addAssetNowRoute(Money, answers, draftId)),
      (answers.assets.propertyOrLand, MAX_PROPERTY_OR_LAND_ASSETS, addAssetNowRoute(PropertyOrLand, answers, draftId)),
      (answers.assets.shares, MAX_SHARES_ASSETS, addAssetNowRoute(Shares, answers, draftId)),
      (answers.assets.business, MAX_BUSINESS_ASSETS, addAssetNowRoute(Business, answers, draftId)),
      (answers.assets.partnerShip, MAX_PARTNERSHIP_ASSETS, addAssetNowRoute(Partnership, answers, draftId)),
      (answers.assets.other, MAX_OTHER_ASSETS, addAssetNowRoute(Other, answers, draftId)),
      (answers.assets.nonEEABusiness, MAX_NON_EEA_BUSINESS_ASSETS, addAssetNowRoute(NonEeaBusiness, answers, draftId))
    )
      .filter(_._1.isDefined)
      .map(x => AssetRoute(x._1.getOrElse(Nil).size, x._2, x._3))

    routes
      .filter(x => x.size < x.maxSize) match {
      case x :: Nil => x.route
      case _        => routeToIndex(answers, controllers.asset.routes.WhatKindOfAssetController.onPageLoad, draftId)
    }
  }

  def addAssetNowRoute(
    `type`: WhatKindOfAsset,
    answers: UserAnswers,
    draftId: String,
    index: Option[Int] = None
  ): Call =
    `type` match {
      case Money          => routeToMoneyIndex(answers, draftId, index)
      case PropertyOrLand => routeToPropertyOrLandIndex(answers, draftId, index)
      case Shares         => routeToSharesIndex(answers, draftId, index)
      case Business       => routeToBusinessIndex(answers, draftId, index)
      case Partnership    => routeToPartnershipIndex(answers, draftId, index)
      case Other          => routeToOtherIndex(answers, draftId, index)
      case NonEeaBusiness => routeToNonEeaBusinessIndex(answers, draftId, index)
    }

  private def routeToMoneyIndex(answers: UserAnswers, draftId: String, index: Option[Int]): Call =
    routeToIndex(answers, controllers.asset.money.routes.AssetMoneyValueController.onPageLoad, draftId, index)

  private def routeToPropertyOrLandIndex(answers: UserAnswers, draftId: String, index: Option[Int]): Call =
    routeToIndex(
      answers,
      controllers.asset.property_or_land.routes.PropertyOrLandAddressYesNoController.onPageLoad,
      draftId,
      index
    )

  private def routeToSharesIndex(answers: UserAnswers, draftId: String, index: Option[Int]): Call =
    routeToIndex(answers, controllers.asset.shares.routes.SharesInAPortfolioController.onPageLoad, draftId, index)

  private def routeToBusinessIndex(answers: UserAnswers, draftId: String, index: Option[Int]): Call =
    routeToIndex(answers, controllers.asset.business.routes.BusinessNameController.onPageLoad, draftId, index)

  private def routeToPartnershipIndex(answers: UserAnswers, draftId: String, index: Option[Int]): Call =
    routeToIndex(
      answers,
      controllers.asset.partnership.routes.PartnershipDescriptionController.onPageLoad,
      draftId,
      index
    )

  private def routeToOtherIndex(answers: UserAnswers, draftId: String, index: Option[Int]): Call =
    routeToIndex(answers, controllers.asset.other.routes.OtherAssetDescriptionController.onPageLoad, draftId, index)

  private def routeToNonEeaBusinessIndex(answers: UserAnswers, draftId: String, index: Option[Int]): Call =
    routeToIndex(answers, controllers.asset.noneeabusiness.routes.NameController.onPageLoad, draftId, index)

  private def routeToIndex[T <: AssetViewModel](
    answers: UserAnswers,
    route: (Int, String) => Call,
    draftId: String,
    index: Option[Int] = None
  ): Call = {
    val i = index.getOrElse {
      answers.get(sections.Assets).getOrElse(List.empty) match {
        case x if !answers.isTaxable =>
          // Answers includes an in progress non-EEA business asset as we have just set the value in WhatKindOfAssetPage.
          // Therefore we need the index to correspond to that asset (i.e. assets.size - 1)
          x.size - 1
        case x                       => x.size
      }
    }
    route(i, draftId)
  }
}
