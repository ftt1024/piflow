package cn.piflow.bundle.kafka

import java.util

import cn.piflow.{JobContext, JobInputStream, JobOutputStream, ProcessContext}
import cn.piflow.conf._
import cn.piflow.conf.bean.PropertyDescriptor
import cn.piflow.conf.util.{ImageUtil, MapUtil}
import java.util.Properties

import org.apache.spark.sql.SparkSession
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord

import scala.collection.mutable

class WriteToKafka extends ConfigurableStop{
  val description: String = "Write data to kafka"
  val inportList: List[String] = List(PortEnum.DefaultPort.toString)
  val outportList: List[String] = List(PortEnum.NonePort.toString)
  var kafka_host:String =_
  var topic:String=_

  def perform(in: JobInputStream, out: JobOutputStream, pec: JobContext): Unit = {
    val spark = pec.get[SparkSession]()
    val df = in.read()
    val properties:Properties  = new Properties()
    properties.put("bootstrap.servers", kafka_host)
    properties.put("acks", "all")
    //properties.put("retries", 0)
    //properties.put("batch.size", 16384)
    //properties.put("linger.ms", 1)
    //properties.put("buffer.memory", 33554432)
    properties.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    properties.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    var producer:Producer[String,String]  = new KafkaProducer[String,String](properties)

    df.collect().foreach(row=>{
      //var hm:util.HashMap[String,String]=new util.HashMap()
      //row.schema.fields.foreach(f=>(if(!f.name.equals(column_name)&&row.getAs(f.name)!=null)hm.put(f.name,row.getAs(f.name).asInstanceOf[String])))
      var res:List[String]=List()
      row.schema.fields.foreach(f=>{
          if(row.getAs(f.name)==null)res="None"::res
          else{
            res=row.getAs(f.name).asInstanceOf[String]::res
          }
        })
      val s:String=res.reverse.mkString(",")
      val record=new ProducerRecord[String,String](topic,s)
      producer.send(record)
    })
    producer.close()
  }


  def initialize(ctx: ProcessContext): Unit = {

  }


  def setProperties(map: Map[String, Any]): Unit = {
    kafka_host=MapUtil.get(map,key="kafka_host").asInstanceOf[String]
    //port=Integer.parseInt(MapUtil.get(map,key="port").toString)
    topic=MapUtil.get(map,key="topic").asInstanceOf[String]
  }

  override def getPropertyDescriptor(): List[PropertyDescriptor] = {
    var descriptor : List[PropertyDescriptor] = List()
    val kafka_host = new PropertyDescriptor().name("kafka_host").displayName("KAFKA_HOST").defaultValue("").required(true)
    val topic = new PropertyDescriptor().name("topic").displayName("TOPIC").defaultValue("").required(true)
    descriptor = kafka_host :: descriptor
    descriptor = topic :: descriptor
    descriptor
  }

  override def getIcon(): Array[Byte] = {
    ImageUtil.getImage("icon/kafka/WriteToKafka.png")
  }

  override def getGroup(): List[String] = {
    List(StopGroup.KafkaGroup.toString)
  }

  override val authorEmail: String = "06whuxx@163.com"
}
