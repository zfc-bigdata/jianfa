package com.atguigu.gmall0317.realtime.app

import java.text.SimpleDateFormat
import java.util.Date

import com.alibaba.fastjson.{JSON, JSONObject}
import com.atguigu.gmall0317.realtime.util.{MyKafkaUtil, RedisUtil}
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.spark.SparkConf
import org.apache.spark.streaming.dstream.InputDStream
import org.apache.spark.streaming.{Seconds, StreamingContext}

import scala.collection.mutable.ListBuffer

object DauApp {

  def main(args: Array[String]): Unit = {
    val sparkConf = new SparkConf().setAppName("dau_app").setMaster("local[4]")
    val ssc = new StreamingContext(sparkConf, Seconds(5))

    val topic = "GMALL0317_STARTUP"
    val groupId = "dau_app_group"

    val inputDstream: InputDStream[ConsumerRecord[String, String]] = MyKafkaUtil.getKafkaStream(topic, ssc, groupId)

    val jsonObjDstream = inputDstream.map { record =>
      val jsonString = record.value()
      val jsonObj = JSON.parseObject(jsonString)
      val ts = jsonObj.getLong("ts")
      val dateFormat = new SimpleDateFormat("yyyy-MM-dd HH")
      val dateTimeStr = dateFormat.format(new Date(ts))
      val dateTimeArr = dateTimeStr.split(" ")
      val dt = dateTimeArr(0)
      val hr = dateTimeArr(1)
      jsonObj.put("dt", dt)
      jsonObj.put("hr", hr)
      jsonObj
    }
    val filteredDstream = jsonObjDstream.mapPartitions { jsonObjItr =>
      val jedis = RedisUtil.getJedisClient
      val jsonList = jsonObjItr.toList
      println("过滤前" + jsonList.size)
      val filteredList = new ListBuffer[JSONObject]
      for (jsonObj <- jsonList) {
        val dauKey = "dau:" + jsonObj.get("dt")
        val mid = jsonObj.getJSONObject("common").getString("mid")
        val ifNonExists = jedis.sadd(dauKey, mid)
        if (ifNonExists == 1) {
          filteredList += jsonObj
        }
      }
      jedis.close()
      println("过滤后：" + filteredList.size)
      filteredList.toIterator
    }
    filteredDstream.print(100)

    ssc.start()
    ssc.awaitTermination()
  }
}
