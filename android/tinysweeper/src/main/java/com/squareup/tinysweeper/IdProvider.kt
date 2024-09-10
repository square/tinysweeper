package com.squareup.tinysweeper

interface IdProvider {
  fun getLabel(): String

  fun getId(): String
}
