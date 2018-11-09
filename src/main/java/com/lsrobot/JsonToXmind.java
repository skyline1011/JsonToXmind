package com.lsrobot;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmind.core.*;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class JsonToXmind {
    private static Logger log = LoggerFactory.getLogger(JsonToXmind.class);
    private static IWorkbook workbook;
    private static List<String> fileName = new ArrayList<>();//路径中所有json文件集合

    public static void main(String[] args) {

        JSONObject jsonObject = new JSONObject();//json文件内容
        ITopic root = null;

        String jsonPath = System.getProperty("user.dir") + File.separator + "data" + File.separator;
        log.info("获取文件根路径：" + jsonPath);
        fileName = getJsonFileList(jsonPath);
        System.out.println("-------------------------------------------------------");
        for(String file : fileName){
            log.info("开始读取Json文件："+ file);
            String json = readJson(file);

            if (json==null||"".equals(json)){
                log.error("该json文件为空");
            }else {
                jsonObject = JSONObject.parseObject(json);
                log.info("开始初始化xmind");
                String rootName = getFileName(file);
                root = initXmind(rootName);

                log.info("开始将Json解析并写入xmind！");
                parseJson2Xmind(jsonObject, root);

                //保存文件：保存在同路径
                File file1 = new File(file);
                String path = file1.getParent();
                save(path + File.separator + rootName + ".xmind");
            }
        }
    }

    /**
     * 获取文件类型为json的文件
     * @param path
     * @return
     */
    public static List<String> getJsonFileList(String path){
        File file = new File(path);
//        List<String> jsonStrs = new ArrayList<>();
        if(file.exists()){
            File[] files = file.listFiles();
            if(null==files || files.length==0){
                log.warn("文件夹是空的!");
                return null;
            }else {
                for(File file1 : files){
                    String tempPath = file1.getAbsolutePath();//D:\IdeaProjects\jsonToXmind\data\aa.txt
//                    String tempName = file1.getName();//aa.txt
                    if(file1.isDirectory()){/*若是文件夹，则递归循环*/
                        log.info("文件夹:" + tempPath);
                        getJsonFileList(tempPath);
                    }else {/*若是文件,则判断是否是json，若是json文件，则把文件名称添加到list*/
                        if(tempPath.endsWith("json")){
                            log.info("文件:" + tempPath);
                            fileName.add(tempPath);
                        }
                    }
                }
            }
            return fileName;
        }else {
            log.warn("文件不存在!");
            return null;
        }
    }

    /**
     * 获取文件名称
     * @param path
     * @return
     */
    public static String getFileName(String path){
        File file = new File(path);
        String fileName = file.getName();
        return fileName.split("\\.")[0];
    }

    /**
     * 读取Json内容
     * @param jsonPath
     * @return
     */
    public static String readJson(String jsonPath){
        StringBuilder sb = new StringBuilder();

        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(jsonPath))));
            String line = null;
            while ((line=br.readLine())!=null){
                sb.append(line);
            }
        } catch (FileNotFoundException e) {
            log.error("读取Json文件失败",e);
        } catch (IOException e) {
            log.error("读取Json文件失败",e);
        }
        return sb.toString();
    }

    /**
     * 初始化xmind
     * @param rootName
     * @return
     */
    public static ITopic initXmind(String rootName) {
        IWorkbookBuilder builder = Core.getWorkbookBuilder();//初始化builder
        workbook = builder.createWorkbook();
        ISheet sheet = workbook.getPrimarySheet();
        ITopic rootTopic = sheet.getRootTopic();
        rootTopic.setTitleText(rootName);
        return rootTopic;
    }

    /**
     * 将Json文件转为xmind
     * @param jsonObject
     * @param root
     */
    public static void parseJson2Xmind(JSONObject jsonObject, ITopic root) {
        if(null != jsonObject){
            Set<String> keys = jsonObject.keySet();
            for(String key : keys){
                log.info("开始处理对象：" +key);
                Object obj = jsonObject.get(key);
                if(obj instanceof JSONObject){
                    JSONObject keyJsonObj = (JSONObject) obj;
                    ITopic topic = workbook.createTopic();
                    topic.setTitleText(key);
                    root.add(topic);
                    if (keyJsonObj.keySet()==null){
                        topic.setTitleText(key+":"+keyJsonObj.getString(key));
                    }else{
                        parseJson2Xmind(keyJsonObj,topic);//递归
                    }
                }else if(obj instanceof JSONArray){
                    JSONArray array=(JSONArray)obj;
                    List<JSONObject> jsonObjectsList = array.toJavaList(JSONObject.class);//JsonArray转List
                    ITopic topic = workbook.createTopic();
                    topic.setTitleText(key);
                    root.add(topic);
                    int i=1;
                    for (JSONObject listObj : jsonObjectsList) {
                        ITopic subTopic = workbook.createTopic();
                        subTopic.setTitleText(key + (i++));
                        topic.add(subTopic);
                        if (listObj.keySet() == null) {
                            topic.setTitleText(key + ":" + listObj.getString(key));
                        } else {
                            parseJson2Xmind(listObj, subTopic);
                        }
                    }
                }else {
                    ITopic topic = workbook.createTopic();
                    topic.setTitleText(key+":"+obj.toString());
                    root.add(topic);
                }
            }
        }else {
            log.info("这是一个空的Json文件！文件名称：" + jsonObject);
        }
    }

    /**
     * 保存xmind文件
     * @param xmindPath
     */
    public static void save(String xmindPath){
        try {
            FileOutputStream stream = new FileOutputStream(xmindPath);
            ISerializer serializer = Core.getWorkbookBuilder().newSerializer();
            serializer.setWorkbook(workbook);
            serializer.setOutputStream(stream);
            serializer.serialize(null);
            log.info("保存到xmind成功！文件：" + xmindPath);
        } catch (FileNotFoundException e) {
            log.error("保存失败！" + e);
            e.printStackTrace();
        } catch (CoreException e) {
            log.error("保存失败！" + e);
            e.printStackTrace();
        } catch (IOException e) {
            log.error("保存失败！" + e);
            e.printStackTrace();
        }
    }
}
