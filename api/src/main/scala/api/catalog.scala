package api

import caliban.GraphQL.graphQL
import caliban.RootResolver
import core.*
import zio.ZIO
import zio.random.Random
import caliban.schema.Annotations.GQLDeprecated
import caliban.schema.{GenericSchema, Schema}

import caliban.*

import java.util.UUID

object catalog:

  def resolver(storeManager: StoreManager): RootResolver[CatalogQueries, CatalogMutations, Unit] =
    RootResolver(
      catalog.CatalogQueries(storeManager.listAllArticles, storeManager.listAllArticles),
      catalog.CatalogMutations(storeManager.addArticle, storeManager.updateArticlePrice, storeManager.removeArticle),
    )

  case class CatalogQueries(
    @GQLDeprecated("Use `catalog`")
    articles: ZIO[Any, Error, List[Ident[Article]]],
    catalog: ZIO[Any, Error, List[Ident[Article]]],
  )

  case class CatalogMutations(
    addArticle: AddArticleForm => ZIO[Random, Error, Ident[Article]],
    updatePrice: UpdateArticlePriceForm => ZIO[Any, Error, Ident[Article]],
    removeArticle: UUID => ZIO[Any, Error, Ident[Article]],
  )
