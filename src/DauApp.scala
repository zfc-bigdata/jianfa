package com.atguigu.gmall0317.realtime.test.app

import java.lang
import java.text.SimpleDateFormat
import java.util.Date

import com.alibaba.fastjson.{JSON, JSONObject}
import com.atguigu.gmall0317.realtime.util.{MyKafkaUtil, OffsetManager, RedisUtil}
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.TopicPartition
import org.apache.spark.SparkConf
import org.apache.spark.streaming.dstream.{DStream, InputDStream}
import org.apache.spark.streaming.kafka010.{HasOffsetRanges, OffsetRange}
import org.apache.spark.streaming.{Seconds, StreamingContext}
import redis.clients.jedis.Jedis

import scala.collection.mutable.ListBuffer

// dau= daily active user     uv = user view , pv = page view
object DauApp {

  def main(args: Array[String]): Unit = {
    val sparkConf: SparkConf = new SparkConf().setAppName("dau_app").setMaster("local[4]")
    val ssc = new StreamingContext(sparkConf, Seconds(5))
    val topic = "GMALL0317_STARTUP"
    val groupId = "dau_app_group"
    var inputDstream: InputDStream[ConsumerRecord[String, String]]=null
    val offsetMap: Map[TopicPartition, Long] = OffsetManager.getOffset(topic,groupId )
    if(offsetMap!=null){
      inputDstream = MyKafkaUtil.getKafkaStream(topic, ssc,offsetMap, groupId)
    }else{
       inputDstream = MyKafkaUtil.getKafkaStream(topic, ssc,  groupId)
    }


    //从流中rdd 获得偏移量的结束点 数组
    var offsetRanges: Array[OffsetRange]=null
    val inputWithOffsetDstream: DStream[ConsumerRecord[String, String]] = inputDstream.transform { rdd =>
      offsetRanges = rdd.asInstanceOf[HasOffsetRanges].offsetRanges
      rdd
    }


    //inputDstream.map(_.value()).print(100)

    //前置处理 1 结构化  2 日期 3 小时
    val jsonObjDstream: DStream[JSONObject] = inputWithOffsetDstream.map { record =>
      val jsonString: String = record.value()
      val jsonObj: JSONObject = JSON.parseObject(jsonString)
      val ts: lang.Long = jsonObj.getLong("ts")
      val dateFormat = new SimpleDateFormat("yyyy-MM-dd HH")
      val dateTimeStr: String = dateFormat.format(new Date(ts))
      val dateTimeArr: Array[String] = dateTimeStr.split(" ")
      val dt: String = dateTimeArr(0)
      val hr: String = dateTimeArr(1)
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
    val filteredDstream: DStream[JSONObject] = jsonObjDstream.mapPartitions { jsonObjItr =>
    val jedis: Jedis = RedisUtil.getJedisClient // 1 连接池
    val jsonList: List[JSONObject] = jsonObjItr.toList
    println("过滤前："+jsonList.size )
    val filteredList = new ListBuffer[JSONObject]
      for (jsonObj <- jsonList) {
        val dauKey = "dau:" + jsonObj.get("dt")
        val mid = jsonObj.getJSONObject("common").getString("mid")
        val ifNonExists: lang.Long = jedis.sadd(dauKey, mid)
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
