package com.riseofcat.client

import com.riseofcat.share.mass.*

interface IClientModel {
  val playerName:String
  fun ready():Boolean
  fun move(direction:Angle)
  fun newCar()
  fun dispose()
}