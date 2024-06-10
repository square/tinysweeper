package com.squareup.tinysweeper.tsengine

interface IdProvider {
  fun getLabel(): String

  fun getId(): String
}
