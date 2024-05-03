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

import models.Constants._
import models.WhatKindOfAsset.{Business, Money, NonEeaBusiness, Other, Partnership, PropertyOrLand, Shares, prefix}
import viewmodels._

case class AssetViewModels(
  monetary: Option[List[MoneyAssetViewModel]],
  propertyOrLand: Option[List[PropertyOrLandAssetViewModel]],
  shares: Option[List[ShareAssetViewModel]],
  business: Option[List[BusinessAssetViewModel]],
  partnerShip: Option[List[PartnershipAssetViewModel]],
  other: Option[List[OtherAssetViewModel]],
  nonEEABusiness: Option[List[NonEeaBusinessAssetViewModel]]
) {

  def nonMaxedOutOptions: List[AssetSize] =
    assetSizes
      .filterNot(x => x.size >= x.maxSize)

  def maxedOutOptions: List[(String, Int)] =
    assetSizes
      .filter(x => x.size >= x.maxSize)
      .map(x => (s"$prefix.${x.kindOfAsset}", x.size))

  def assetSizes: List[AssetSize] = List(
    (Money, monetary, MAX_MONEY_ASSETS),
    (PropertyOrLand, propertyOrLand, MAX_PROPERTY_OR_LAND_ASSETS),
    (Shares, shares, MAX_SHARES_ASSETS),
    (Business, business, MAX_BUSINESS_ASSETS),
    (NonEeaBusiness, nonEEABusiness, MAX_NON_EEA_BUSINESS_ASSETS),
    (Partnership, partnerShip, MAX_PARTNERSHIP_ASSETS),
    (Other, other, MAX_OTHER_ASSETS)
  )
    .filter(_._2.isDefined)
    .map(x => AssetSize(x._1, x._2.getOrElse(Nil).size, x._3))

}
