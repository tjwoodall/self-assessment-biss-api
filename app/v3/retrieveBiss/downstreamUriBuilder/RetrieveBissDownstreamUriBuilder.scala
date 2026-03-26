/*
 * Copyright 2025 HM Revenue & Customs
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

package v3.retrieveBiss.downstreamUriBuilder

import api.connectors.DownstreamUri
import api.connectors.DownstreamUri.IfsUri
import api.models.domain.{BusinessId, Nino, TaxYear}
import api.models.downstream.IncomeSourceType
import config.AppConfig
import v3.retrieveBiss.model.response.RetrieveBissResponse

sealed trait RetrieveBissDownstreamUriBuilder[Resp] {

  def buildUri(nino: Nino, businessId: BusinessId, incomeSourceType: IncomeSourceType, taxYear: TaxYear)(implicit
      appConfig: AppConfig): (DownstreamUri[Resp], Seq[(String, String)])

}

object RetrieveBissDownstreamUriBuilder {

  case object Api1415 extends RetrieveBissDownstreamUriBuilder[RetrieveBissResponse] {

    override def buildUri(
        nino: Nino,
        businessId: BusinessId,
        incomeSourceType: IncomeSourceType,
        taxYear: TaxYear
    )(implicit appConfig: AppConfig): (DownstreamUri[RetrieveBissResponse], Seq[(String, String)]) = {

      val uri =
        IfsUri[RetrieveBissResponse](
          s"income-tax/income-sources/nino/$nino/$incomeSourceType/${taxYear.asDownstream}/biss"
        )

      val queryParams = Seq("incomeSourceId" -> businessId.businessId)

      (uri, queryParams)
    }

  }

  case object Api1871 extends RetrieveBissDownstreamUriBuilder[RetrieveBissResponse] {

    override def buildUri(
        nino: Nino,
        businessId: BusinessId,
        incomeSourceType: IncomeSourceType,
        taxYear: TaxYear
    )(implicit appConfig: AppConfig): (DownstreamUri[RetrieveBissResponse], Seq[(String, String)]) = {

      val uri =
        IfsUri[RetrieveBissResponse](
          s"itsa/income-tax/v1/income-sources/${taxYear.asTysDownstream}/$nino/$businessId/$incomeSourceType/biss"
        )

      (uri, Nil)
    }

  }

  case object Api1879 extends RetrieveBissDownstreamUriBuilder[RetrieveBissResponse] {

    override def buildUri(
        nino: Nino,
        businessId: BusinessId,
        incomeSourceType: IncomeSourceType,
        taxYear: TaxYear
    )(implicit appConfig: AppConfig): (DownstreamUri[RetrieveBissResponse], Seq[(String, String)]) = {

      val uri =
        IfsUri[RetrieveBissResponse](
          s"itsa/income-tax/v1/${taxYear.asTysDownstream}/income-sources/$nino/$businessId/$incomeSourceType/biss"
        )

      (uri, Nil)
    }

  }

  def downstreamUriFor[Resp](taxYear: TaxYear): RetrieveBissDownstreamUriBuilder[Resp] = {
    val downstreamUriBuilder: RetrieveBissDownstreamUriBuilder[RetrieveBissResponse] = taxYear.year match {
      case year if year >= 2026               => Api1879
      case _ if taxYear.useTaxYearSpecificApi => Api1871
      case _                                  => Api1415
    }

    downstreamUriBuilder.asInstanceOf[RetrieveBissDownstreamUriBuilder[Resp]]
  }

}
