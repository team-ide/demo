package com.teamide.demo;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;
import org.apache.http.ParseException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.InputLocation;
import org.apache.maven.model.License;
import org.apache.maven.model.Model;
import org.apache.maven.model.Prerequisites;
import org.apache.maven.model.Scm;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.poi.ss.usermodel.Workbook;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringInputStream;
import org.codehaus.plexus.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

public class Main {
    //    final static String ROOT_POM = "C:\\Users\\Administrator\\.m2\\repository\\com\\vrv\\vrv-root-dependencies\\1.0.2\\vrv-root-dependencies-1.0.2.pom";
    final static String ROOT_POM = "D:\\Workspaces\\Code\\linkdood\\standard\\server\\IMServerRoot\\pom.xml";
    final static String LIB_DIR = "D:\\Workspaces\\libs";

    public static final MavenXpp3Reader reader = new MavenXpp3Reader();
    static Map<String, Dependency> dependencyMap = getRootDependencyMap();

    public static void main(String[] args) throws Exception {
        File libDir = new File(LIB_DIR);
        File[] fileList = libDir.listFiles();
        List<Map<String, String>> outs = new ArrayList<>();
        List<Dependency> dependencyList = new ArrayList<>();
        int count = fileList.length;
        int index = 0;
        for (File one : fileList) {
            index++;
            System.out.println("------------------");
            System.out.println("共" + count + "个依赖，解析第" + index + "个：" + one.getName());
            Model model = getModelByJarFile(one);
            if (model == null) {
                System.out.println(one.getName() + " pom not exist");
                continue;
            }
            if (StringUtils.isEmpty(model.getGroupId())
                    || StringUtils.isEmpty(model.getArtifactId())
                    || StringUtils.isEmpty(model.getVersion())) {
                System.out.println(one.getName() + " pom groupId is null");
                return;
            }
            if (model.getGroupId().startsWith("com.vrv")) {
                continue;
            }
            Dependency dependency = new Dependency();
            dependency.setGroupId(model.getGroupId());
            dependency.setArtifactId(model.getArtifactId());
            dependency.setVersion(model.getVersion());
            dependencyList.add(dependency);

            Map<String, String> out = new HashMap<>();
            outs.add(out);

            out.put("name", model.getArtifactId());
            out.put("groupId", model.getGroupId());
            out.put("artifactId", model.getArtifactId());
            out.put("version", model.getVersion());

            out.put("url", model.getUrl());
            if (model.getScm() != null) {
                out.put("sourceUrl", model.getScm().getUrl());
//                System.out.println("Source Page:" + pomModel.getScm().getUrl());
            }
            if (model.getLicenses() != null) {
                if (model.getLicenses().size() > 0) {
                    out.put("licenseName", model.getLicenses().get(0).getName());
                    out.put("licenseUrl", model.getLicenses().get(0).getUrl());
                    out.put("licenseDistribution", model.getLicenses().get(0).getDistribution());
                    out.put("licenseComments", model.getLicenses().get(0).getComments());
                }
            }
            if (model.getOrganization() != null) {
                out.put("organizationName", model.getOrganization().getName());
                out.put("organizationUrl", model.getOrganization().getUrl());
//                System.out.println("Organization:" + pomModel.getOrganization().getName());
            }
            out.put("description", model.getDescription());

            String q = "g:" + model.getGroupId() + " AND a:" + model.getArtifactId() + "";
            String url = "https://search.maven.org/solrsearch/select";
            Map<String, String> params = new HashMap<>();
            params.put("q", q);
            params.put("core", "gav");
            params.put("rows", "1");
            String infoStr = doGet(url, params);
            if (StringUtils.isNotEmpty(infoStr)) {
                Map<String, Object> info = stringToObject(infoStr, HashMap.class);
                if (info.get("response") != null) {
                    Map<String, Object> response = (Map<String, Object>) info.get("response");
                    if (response.get("docs") != null) {
                        List<Map<String, Object>> docs = (List<Map<String, Object>>) response.get("docs");
                        if (docs.size() > 0) {
//                            System.out.println("Last Version:" + docs.get(0).get("v"));
                            String lastVersion = docs.get(0).get("v").toString();
                            out.put("lastVersion", lastVersion);
                        }
                    }
                }
            }
            if (StringUtils.isEmpty(out.get("lastVersion"))) {
                out.put("lastVersion", out.get("version"));
            }
        }
        List<List<Object>> data = new ArrayList<>();
        for (int i = 0; i < outs.size(); i++) {
            Map<String, String> out = outs.get(i);
            List<Object> one = new ArrayList<>();
            one.add(i + 1);
            one.add(out.get("name"));
            one.add(out.get("version"));
            if (StringUtils.isNotEmpty(out.get("sourceUrl"))) {
                one.add(out.get("sourceUrl"));
            } else {
                one.add(out.get("url"));
            }
            one.add(out.get("licenseName"));
            one.add(out.get("licenseUrl"));
            one.add(out.get("licenseComments"));
            one.add(out.get("lastVersion"));
            one.add(("否"));
            one.add(("否"));
            String openUsage = null;
            String licenseInfo = "";
            if (out.get("licenseName") != null) {
                licenseInfo += out.get("licenseName");
            }
            if (out.get("licenseUrl") != null) {
                licenseInfo += "-" + out.get("licenseUrl");
            }
            if (licenseInfo.toUpperCase().contains("APACHE")) {
                openUsage = "Apache";
            } else if (licenseInfo.toUpperCase().contains("BSD")) {
                openUsage = "BSD";
            }
            if (licenseInfo.toUpperCase().contains("MIT")) {
                openUsage = "MIT";
            } else if (licenseInfo.toUpperCase().contains("MPL")) {
                openUsage = "MPL";
            } else if (licenseInfo.toUpperCase().contains("EPL")) {
                openUsage = "EPL";
            } else if (licenseInfo.toUpperCase().contains("LGPL")) {
                openUsage = "LGPL";
            } else if (licenseInfo.toUpperCase().contains("GPL")) {
                openUsage = "GPL";
            } else if (licenseInfo.toUpperCase().contains("AGPL")) {
                openUsage = "AGPL";
            }
            openUsage = "动态链接";
            one.add(openUsage);
            data.add(one);
        }
        String dirPath = "D:\\Workspaces\\" + new Date().getTime();
        File outDir = new File(dirPath);
        outDir.mkdirs();
        File templateExcelFile = new File("D:\\Workspaces\\开源软件.xlsx");
        File excelFile = new File(outDir, "开源软件.xlsx");
        excelFile.createNewFile();
        Workbook book = ExcelUtils.getWorkbookFromExcel(templateExcelFile);
        ExcelUtils.writeDataToWorkbook(null, data, book, 0, 2);
        FileOutputStream os = new FileOutputStream(excelFile);
        ExcelUtils.writeWorkbookToOutputStream(book, os);
//        ExcelUtils.writeDataToTemplateOutputStream(excelTemplate, data, os);
        os.close();

        StringBuilder dependenciesContext = new StringBuilder();

        for (Dependency dependency : dependencyList) {
            dependenciesContext.append("\t\t").append("<dependency>").append("\n");
            dependenciesContext.append("\t\t\t").append("<groupId>").append(dependency.getGroupId()).append("</groupId>").append("\n");
            dependenciesContext.append("\t\t\t").append("<artifactId>").append(dependency.getArtifactId()).append("</artifactId>").append("\n");
            dependenciesContext.append("\t\t\t").append("<version>").append(dependency.getVersion()).append("</version>").append("\n");
            dependenciesContext.append("\t\t").append("</dependency>").append("\n");
        }

        String content = FileUtils.fileRead("D:\\Workspaces\\template.pom.xml");
        content = content.replace("${dependencies}", dependenciesContext.toString());
        File pomFile = new File(outDir, "pom.xml");
        pomFile.createNewFile();
        FileUtils.fileWrite(pomFile, content);

        Process process = Runtime.getRuntime().exec("cmd");
        InputStream inputStream = process.getInputStream();
        OutputStream outputStream = process.getOutputStream();
        new Thread() {
            @Override
            public void run() {
                try {
                    outputStream.write(("cd " + dirPath + "\n").getBytes(StandardCharsets.UTF_8));
                    outputStream.flush();
                    outputStream.write(("mvn verify" + "\n").getBytes(StandardCharsets.UTF_8));
                    outputStream.flush();
                } catch (Exception e) {

                }
            }
        }.start();
//        "mvn verify"
        byte[] bs = new byte[1024];
        int readed = -1;
        while ((readed = inputStream.read(bs)) >= 0) {
            System.out.print(new String(bs, 0, readed, StandardCharsets.UTF_8));
        }
    }

    public static Model getModelByPom(File pomFile) throws Exception {
        String content = FileUtils.fileRead(pomFile);
        return getModelByPom(content);
    }

    public static Model getModelByPom(InputStream in) throws Exception {
        String content = IOUtil.toString(in);
        in.close();
        return getModelByPom(content);
    }

    public static Model getModelByPom(String content) throws Exception {
        ByteArrayInputStream in = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        Model pomModel = reader.read(in);
        in.close();
        formatModel(pomModel, content);
        return pomModel;
    }

    public static void formatModel(Model model, String content) throws Exception {
        Properties properties = model.getProperties();
        if (model.getParent() != null) {
            if (StringUtils.isEmpty(model.getGroupId())) {
                model.setGroupId(model.getParent().getGroupId());
            }
            if (StringUtils.isEmpty(model.getVersion())) {
                model.setVersion(model.getParent().getVersion());
            }
            String filepath = getPomFilePath(model.getParent().getGroupId(), model.getParent().getArtifactId(), model.getParent().getVersion());
            // 查找本地
            File subPomFile = getPomFileByPath(filepath);
            if (subPomFile.exists()) {
                appendParentProperties(model, properties);
            } else {
                System.out.println(model.getGroupId() + ":" + model.getArtifactId() + ":" + model.getVersion() + " parent pom not exist");
                throw new Exception(subPomFile.toURI().getPath() + " not exist");
            }
        }
        if (model.getUrl() != null) {
            model.setUrl(formatPropertyValue(model, model.getUrl()));
        }
        if (model.getUrl() == null) {
            model.setUrl(getModelParentUrl(model));
        }
        if (model.getScm() != null && model.getScm().getUrl() != null) {
            model.getScm().setUrl(formatPropertyValue(model, model.getScm().getUrl()));
        }
        if (model.getScm() == null || model.getScm().getUrl() == null) {
            model.setScm(getModelParentScm(model));
        }
        if (model.getLicenses() == null || model.getLicenses().size() == 0) {
            List<License> parentLicenses = getModelParentLicenses(model);
            if (parentLicenses != null && parentLicenses.size() > 0) {
                model.setLicenses(parentLicenses);
            } else {

                License license = null;
                if (content.toLowerCase().contains("http://www.apache.org/licenses/license-2.0")) {
                    license = new License();
                    license.setName("Apache License 2.0");
                    license.setUrl("http://www.apache.org/licenses/LICENSE-2.0");
                } else if (content.toLowerCase().contains("https://www.apache.org/licenses/license-2.0.txt")) {
                    license = new License();
                    license.setName("Apache License, Version 2.0");
                    license.setUrl("https://www.apache.org/licenses/LICENSE-2.0.txt");
                } else if (content.toLowerCase().contains("http://opensource.org/licenses/mit")) {
                    license = new License();
                    license.setName("The MIT License");
                    license.setUrl("http://opensource.org/licenses/MIT");
                } else if (content.toLowerCase().contains("http://opensource.org/licenses/bsd-3-clause")) {
                    license = new License();
                    license.setName("BSD License");
                    license.setUrl("http://opensource.org/licenses/BSD-3-Clause");
                }

                if (license != null) {
                    model.setLicenses(new ArrayList<>());
                    model.getLicenses().add(license);
                }
            }
        }
    }

    public static Scm getModelParentScm(Model model) throws Exception {
        if (model.getParent() == null) {
            return null;
        }
        try {
            String filepath = getPomFilePath(model.getParent().getGroupId(), model.getParent().getArtifactId(), model.getParent().getVersion());
            // 查找本地
            File parentPomFile = getPomFileByPath(filepath);
            if (parentPomFile.exists()) {
                Model parentModel = getModelByPom(parentPomFile);
                if (parentModel.getScm() == null || parentModel.getScm().getUrl() == null) {
                    return getModelParentScm(parentModel);
                }
                return parentModel.getScm();
            } else {
                throw new Exception(parentPomFile.toURI().getPath() + " not exist");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String getModelParentUrl(Model model) throws Exception {
        if (model.getParent() == null) {
            return null;
        }
        try {
            String filepath = getPomFilePath(model.getParent().getGroupId(), model.getParent().getArtifactId(), model.getParent().getVersion());
            // 查找本地
            File parentPomFile = getPomFileByPath(filepath);
            if (parentPomFile.exists()) {
                Model parentModel = getModelByPom(parentPomFile);
                if (parentModel.getUrl() == null) {
                    return getModelParentUrl(parentModel);
                }
                return parentModel.getUrl();
            } else {
                throw new Exception(parentPomFile.toURI().getPath() + " not exist");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static List<License> getModelParentLicenses(Model model) throws Exception {
        if (model.getParent() == null) {
            return new ArrayList<>();
        }
        try {
            String filepath = getPomFilePath(model.getParent().getGroupId(), model.getParent().getArtifactId(), model.getParent().getVersion());
            // 查找本地
            File parentPomFile = getPomFileByPath(filepath);
            if (parentPomFile.exists()) {
                Model parentModel = getModelByPom(parentPomFile);
                if (parentModel.getLicenses() == null || parentModel.getLicenses().size() == 0) {
                    return getModelParentLicenses(parentModel);
                }
                return parentModel.getLicenses();
            } else {
                throw new Exception(parentPomFile.toURI().getPath() + " not exist");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static File getPomFileByPath(String filepath) {
        File pomFile = new File("C:\\Users\\Administrator\\.m2\\repository\\" + filepath);
        return pomFile;
    }


    public static Model getModelByJarFile(File file) throws Exception {
        String deName = file.getName().substring(0, file.getName().length() - 4);
        Model model = null;
//        System.out.println("jar file dep name:" + deName);
        if (dependencyMap.get(deName) != null) {
            Dependency dependency = dependencyMap.get(deName);
            String filepath = getPomFilePath(dependency);
            // 查找本地
            File pomFile = getPomFileByPath(filepath);
            if (pomFile.exists()) {
                model = getModelByPom(pomFile);
            }
        }
        if (model == null) {

            JarFile jarFile = new JarFile(file);
            List<Model> models = new ArrayList<>();
            jarFile.stream().iterator().forEachRemaining(a -> {
                String name = a.getName();
                if (!name.startsWith("META-INF/maven/")) {
                    return;
                }
                if (!name.endsWith("/pom.xml")) {
                    return;
                }
                ZipEntry zipEntry = jarFile.getEntry(name);
                try {
                    InputStream in = jarFile.getInputStream(zipEntry);
                    models.add(getModelByPom(in));
                    in.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            jarFile.close();
            if (models.size() > 0) {
                model = models.get(0);
            }
        }
        return model;
    }

    public static Map<String, Dependency> getRootDependencyMap() {
        Map<String, Dependency> map = new HashMap<>();
        List<String> loads = new ArrayList<>();
        appendDependencyMap(new File(ROOT_POM), map, loads, null);
        return map;
    }

    public static String getPomFilePath(Dependency dependency) {
        String filepath = dependency.getGroupId().replaceAll("\\.", "/");
        filepath += "/" + dependency.getArtifactId() + "/" + dependency.getVersion();
        filepath += "/" + dependency.getArtifactId() + "-" + dependency.getVersion() + ".pom";
        return filepath;
    }

    public static String getPomFilePath(String groupId, String artifactId, String version) {
        String filepath = groupId.replaceAll("\\.", "/");
        filepath += "/" + artifactId + "/" + version;
        filepath += "/" + artifactId + "-" + version + ".pom";
        return filepath;
    }

    public static void appendParentProperties(Model model, Properties properties) {
        if (model.getParent() == null) {
            return;
        }
        try {
            String filepath = getPomFilePath(model.getParent().getGroupId(), model.getParent().getArtifactId(), model.getParent().getVersion());
            // 查找本地
            File parentPomFile = getPomFileByPath(filepath);
            if (parentPomFile.exists()) {
                Model parentModel = getModelByPom(parentPomFile);
                if (parentModel.getProperties() != null) {
                    Enumeration<Object> keys = parentModel.getProperties().keys();
                    while (keys.hasMoreElements()) {
                        Object key = keys.nextElement();
                        if (properties.get(key) == null) {
                            properties.put(key, parentModel.getProperties().get(key));
                        }
                    }
                }
                appendParentProperties(parentModel, properties);
            } else {
                throw new Exception(parentPomFile.toURI().getPath() + " not exist");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void appendDependencyMap(File pomFile, Map<String, Dependency> map, List<String> loads, Properties cProperties) {
        if (loads.contains(pomFile.toURI().getPath())) {
            return;
        }
        loads.add(pomFile.toURI().getPath());
        try {
            Model model = getModelByPom(pomFile);
            Properties properties = model.getProperties();
            if (cProperties != null) {
                Enumeration<Object> keys = cProperties.keys();
                while (keys.hasMoreElements()) {
                    Object key = keys.nextElement();
                    properties.put(key, cProperties.get(key));
                }
            }
            if (model.getParent() != null) {
                String filepath = getPomFilePath(model.getParent().getGroupId(), model.getParent().getArtifactId(), model.getParent().getVersion());
                // 查找本地
                File subPomFile = getPomFileByPath(filepath);
                if (subPomFile.exists()) {
                    appendDependencyMap(subPomFile, map, loads, properties);
                } else {
                    throw new Exception(subPomFile.toURI().getPath() + " not exist");
                }
            }
            if (model.getDependencies() != null) {
                List<Dependency> dependencies = model.getDependencies();
                appendDependencyMap(model, dependencies, map, loads);
            }
            if (model.getDependencyManagement() != null) {
                List<Dependency> dependencies = model.getDependencyManagement().getDependencies();
                appendDependencyMap(model, dependencies, map, loads);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String formatPropertyValue(Model model, String name) {
        if (name == null) {
            return name;
        }
        if (!name.startsWith("${") || !name.endsWith("}")) {
            return name;
        }
        String key = name.substring(2, name.length() - 1);
        if (key.equals("project.version")) {
            return model.getVersion();
        }
        if (model.getProperties().get(key) == null) {
            return name;
        }
        return model.getProperties().get(key).toString();
    }

    public static void appendDependencyMap(Model model, List<Dependency> dependencies, Map<String, Dependency> map, List<String> loads) {

        try {
            for (Dependency dependency : dependencies) {
                String version = dependency.getVersion();
                if (StringUtils.isEmpty(version)) {
                    continue;
                }
                version = formatPropertyValue(model, version);
                dependency.setVersion(version);
                System.out.println("root dep name:" + dependency.getArtifactId() + "-" + dependency.getVersion());
                map.put(dependency.getArtifactId() + "-" + dependency.getVersion(), dependency);

                String filepath = getPomFilePath(dependency);
                // 查找本地
                File subPomFile = getPomFileByPath(filepath);
                if (subPomFile.exists()) {
                    appendDependencyMap(subPomFile, map, loads, null);
                }
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void byPom() throws Exception {
        FileInputStream in = new FileInputStream(new File(ROOT_POM));
        MavenXpp3Reader reader = new MavenXpp3Reader();
        Model model = reader.read(in);
        in.close();

        Properties properties = model.getProperties();

        List<Map<String, String>> outs = new ArrayList<>();
        List<Dependency> dependencies = model.getDependencyManagement().getDependencies();
        int count = dependencies.size();
        int index = 0;
        for (Dependency dependency : dependencies) {
            index++;
            if (dependency.getGroupId().startsWith("com.vrv")) {
                continue;
            }
            Map<String, String> out = new HashMap<>();
            outs.add(out);
            System.out.println("------------------");
            System.out.println("共" + count + "个依赖，解析第" + index + "个");
            String version = dependency.getVersion();
            if (version.startsWith("${")) {
                String key = version.substring(2, version.length() - 1);
                version = properties.get(key).toString();
                dependency.setVersion(version);
            }
            out.put("name", dependency.getArtifactId());
            out.put("version", dependency.getVersion());

//            System.out.println("GroupId:" + dependency.getGroupId());
//            System.out.println("ArtifactId:" + dependency.getArtifactId());
//            System.out.println("Version:" + dependency.getVersion());
            String q = "g:" + dependency.getGroupId() + " AND a:" + dependency.getArtifactId() + " AND v:" + dependency.getVersion() + " AND p:jar";
            String url = "https://search.maven.org/solrsearch/select";
            Map<String, String> params = new HashMap<>();
            params.put("q", q);
            params.put("rows", "20");
            params.put("wt", "json");
            String infoStr = doGet(url, params);
            if (StringUtils.isNotEmpty(infoStr)) {
                Map<String, Object> info = stringToObject(infoStr, HashMap.class);
                if (info.get("response") != null) {
                    Map<String, Object> response = (Map<String, Object>) info.get("response");
                    if (response.get("docs") != null) {
                        List<Map<String, Object>> docs = (List<Map<String, Object>>) response.get("docs");
                        if (docs.size() > 0) {
//                            System.out.println("Last Version:" + docs.get(0).get("v"));
                            String lastVersion = docs.get(0).get("v").toString();
                            out.put("lastVersion", lastVersion);
                        }
                    }
                }
            }

            url = "https://search.maven.org/remotecontent";
            String filepath = dependency.getGroupId().replaceAll("\\.", "/");
            filepath += "/" + dependency.getArtifactId() + "/" + dependency.getVersion();
            filepath += "/" + dependency.getArtifactId() + "-" + dependency.getVersion() + ".pom";
            params = new HashMap<>();
//            System.out.println("filepath:" + filepath);
            params.put("filepath", filepath);
            String pomStr = doGet(url, params);
            Model pomModel;
            try {
                pomModel = reader.read(new StringReader(pomStr));
            } catch (Exception e) {
                // 查找本地
                File pomFile = new File("C:\\Users\\Administrator\\.m2\\repository\\" + filepath);
                if (pomFile.exists()) {
                    in = new FileInputStream(pomFile);
                    pomModel = reader.read(in);
                    in.close();
                } else {
                    System.err.println("获取" + filepath + "失败：" + e.getMessage());
                    continue;
                }
            }

//            if (StringUtils.isNotEmpty(pomModel.getName())) {
//                String name = pomModel.getName();
//                out.put("name", name);
//            }
//            System.out.println("Home Page:" + pomModel.getUrl());
            out.put("url", pomModel.getUrl());
            if (pomModel.getScm() != null) {
                out.put("sourceUrl", pomModel.getScm().getUrl());
//                System.out.println("Source Page:" + pomModel.getScm().getUrl());
            }
            if (pomModel.getLicenses() != null) {

                for (License license : pomModel.getLicenses()) {
                    out.put("licenseName", license.getName());
                    out.put("licenseUrl", license.getUrl());
                    out.put("licenseDistribution", license.getDistribution());
                    out.put("licenseComments", license.getComments());
//                    System.out.println("License Name:" + license.getName());
//                    System.out.println("License Url:" + license.getUrl());
                }
            }
            if (pomModel.getOrganization() != null) {
                out.put("organizationName", pomModel.getOrganization().getName());
                out.put("organizationUrl", pomModel.getOrganization().getUrl());
//                System.out.println("Organization:" + pomModel.getOrganization().getName());
            }
            out.put("description", pomModel.getDescription());
//            System.out.println("Description:" + pomModel.getDescription());
//            break;
        }

        File excelTemplate = new File("D:\\Workspaces\\开源软件模板.xlsx");
        File excelFile = new File("D:\\Workspaces\\开源软件.xlsx");
        List<List<Object>> data = new ArrayList<>();
        for (int i = 0; i < outs.size(); i++) {
            Map<String, String> out = outs.get(i);
            List<Object> one = new ArrayList<>();
            one.add(i + 1);
            one.add(out.get("name"));
            one.add(out.get("version"));
            if (StringUtils.isNotEmpty(out.get("sourceUrl"))) {
                one.add(out.get("sourceUrl"));
            } else {
                one.add(out.get("url"));
            }
            one.add(out.get("licenseName"));
            one.add(out.get("licenseUrl"));
            one.add(out.get("licenseDistribution"));
            one.add(out.get("lastVersion"));
            one.add(("否"));
            one.add(("否"));
            one.add(("BSD"));
            data.add(one);
        }
        Workbook book = ExcelUtils.getWorkbookFromExcel(excelFile);
        ExcelUtils.writeDataToWorkbook(null, data, book, 0, 2);
        FileOutputStream os = new FileOutputStream(excelFile);
        ExcelUtils.writeWorkbookToOutputStream(book, os);
//        ExcelUtils.writeDataToTemplateOutputStream(excelTemplate, data, os);
        os.close();
    }

    public static String doGet(String url, Map<String, String> params) {
        String res = null;
        // 获得Http客户端(可以理解为:你得先有一个浏览器;注意:实际上HttpClient与浏览器是不一样的)
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();

        // 参数
        StringBuilder urlParams = new StringBuilder();
        for (String key : params.keySet()) {
            String value = params.get(key);
            try {
                if (!key.equals("filepath")) {
                    value = URLEncoder.encode(value, "utf-8");
                }
                // 字符数据最好encoding以下;这样一来，某些特殊字符才能传过去(如:某人的名字就是“&”,不encoding的话,传不过去)
                urlParams.append(key + "=").append(value).append("&");
            } catch (UnsupportedEncodingException e1) {
                e1.printStackTrace();
            }
        }
        url = url + "?" + urlParams.toString();
        // 创建Get请求
        HttpGet httpGet = new HttpGet(url);
        System.out.println("Get Url:" + url);
        // 响应模型
        CloseableHttpResponse response = null;
        try {
            // 配置信息
            RequestConfig requestConfig = RequestConfig.custom()
                    // 设置连接超时时间(单位毫秒)
                    .setConnectTimeout(50000)
                    // 设置请求超时时间(单位毫秒)
                    .setConnectionRequestTimeout(50000)
                    // socket读写超时时间(单位毫秒)
                    .setSocketTimeout(50000)
                    // 设置是否允许重定向(默认为true)
                    .setRedirectsEnabled(true).build();

            // 将上面的配置信息 运用到这个Get请求里
            httpGet.setConfig(requestConfig);

            // 由客户端执行(发送)Get请求
            response = httpClient.execute(httpGet);

            // 从响应模型中获取响应实体
            HttpEntity responseEntity = response.getEntity();
//            System.out.println("响应状态为:" + response.getStatusLine());
            if (responseEntity != null) {
                res = EntityUtils.toString(responseEntity);
//                System.out.println("响应内容长度为:" + responseEntity.getContentLength());
//                System.out.println("响应内容为:" + res);
            }
        } catch (ParseException | IOException e) {
            e.printStackTrace();
        } finally {
            try {
                // 释放资源
                if (httpClient != null) {
                    httpClient.close();
                }
                if (response != null) {
                    response.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return res;
    }


    public static String jsonToString(Object obj) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        String value = mapper.writeValueAsString(obj);
        return value;
    }

    public static <T> T stringToObject(String value, Class<T> clazz) throws JsonProcessingException, JsonMappingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(value, clazz);
    }
}
