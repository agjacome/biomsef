package es.uvigo.ei.sing.biomsef
package controller

import play.api.http.LazyHttpErrorHandler

object AssetsController extends controllers.AssetsBuilder(LazyHttpErrorHandler)
