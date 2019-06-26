package com.tag.helper;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionScopes;
import com.google.api.services.vision.v1.model.*;
import com.google.common.collect.ImmutableList;
import com.tag.constants.ImageTagErrorType;
import com.tag.model.ImageTagJsonModel;
import com.tag.model.TagError;
import com.tag.model.TagModel;
import org.jooq.lambda.tuple.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by jiaqiwu on 2019/6/17.
 */
public class GoogleVisionLabelHelper {

    public static final Logger logger = LoggerFactory.getLogger(GoogleVisionLabelHelper.class);

    private static final String APPLICATION_NAME = "Google-VisionLabelSample/1.0";

    private static final int MAX_LABELS = 100;

    private static Vision vision;

    private static NetHttpTransport netHttpTransport;

    private static NetHttpTransport getNetHttpTransport() {
        if (netHttpTransport == null) {
            netHttpTransport = new NetHttpTransport.Builder().build();
        }
        return netHttpTransport;
    }

    private static Vision getVision() throws IOException {
        if (vision == null) {
            GoogleCredential credential =
                    GoogleCredential.getApplicationDefault().createScoped(VisionScopes.all());
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            vision = new Vision.Builder(getNetHttpTransport(), jsonFactory, credential)
                    .setApplicationName(APPLICATION_NAME)
                    .build();
        }
        return vision;
    }

    public static ImageTagJsonModel getLabelsByUrl(String path) throws IOException {

        byte[] data = toByte(new URL(path));
        long time = System.currentTimeMillis();
        AnnotateImageRequest request =
                new AnnotateImageRequest()
//                        .setImage(new Image().setSource(new ImageSource().setImageUri(path)))
                        .setImage(new Image().encodeContent(data))
                        .setFeatures(ImmutableList.of(
                                new Feature()
                                        .setType("LABEL_DETECTION")
                                        .setMaxResults(MAX_LABELS)));
        Vision.Images.Annotate annotate = getVision().images()
                .annotate(new BatchAnnotateImagesRequest().setRequests(ImmutableList.of(request)));
        // Due to a bug: requests to Vision API containing large images fail when GZipped.
        annotate.setDisableGZipContent(true);

        BatchAnnotateImagesResponse batchResponse = annotate.execute();
        assert batchResponse.getResponses().size() == 1;
        AnnotateImageResponse response = batchResponse.getResponses().get(0);
        logger.info("请求io时长={}ms", System.currentTimeMillis() - time);
        return getResultModel(response, null);
    }

    public static List<ImageTagJsonModel> getLabelsByUrlList(List<Tuple2<Integer, String>> list) throws IOException {

        List<Tuple2<Integer, String>> errorPath = new ArrayList<>();
        List<ImageTagJsonModel> resultList = new ArrayList<>();

        Vision.Images.Annotate annotate = getVision().images()
                .annotate(
                        new BatchAnnotateImagesRequest().setRequests(
                                list.stream().map(e -> {
                                    try {
                                        return new AnnotateImageRequest()
                                                //                        .setImage(new Image().setSource(new ImageSource().setImageUri(path)))
                                                .setImage(new Image().encodeContent(toByte(new URL(e.v2()))))
                                                .setFeatures(ImmutableList.of(
                                                        new Feature()
                                                                .setType("LABEL_DETECTION")
                                                                .setMaxResults(MAX_LABELS)));
                                    } catch (IOException e1) {
                                        errorPath.add(new Tuple2<>(e.v1(), "获取标签io异常"));
                                        logger.error("", e1);
                                        return null;
                                    }
                                }).filter(e -> e != null)
                                        .collect(Collectors.toList())
                        ));
        Set<Integer> errorIds = errorPath.stream().map(Tuple2::v1).collect(Collectors.toSet());
        List<Tuple2<Integer, String>> tempList = list.stream().filter(e -> !errorIds.contains(e.v1())).collect(Collectors.toList());
        annotate.setDisableGZipContent(true);
        BatchAnnotateImagesResponse batchResponse = annotate.execute();
        List<AnnotateImageResponse> responseList = batchResponse.getResponses();
        if (tempList.size() == responseList.size()) {
            for (int i = 0; i < responseList.size(); i++) {
                AnnotateImageResponse response = batchResponse.getResponses().get(i);
                resultList.add(getResultModel(response, tempList.get(i).v1()));
            }
        } else {
            return Collections.emptyList();
        }
        errorPath.forEach(e -> resultList.add(getResultModel(e.v2(), e.v1())));
        return resultList.stream().sorted((a, b) -> Integer.compare(a.getId(), b.getId())).collect(Collectors.toList());
    }


    public static void printLabels(PrintStream out, List<EntityAnnotation> labels) {
        for (EntityAnnotation label : labels) {
            out.printf(
                    "\t%s (score: %.3f)\n",
                    label.getDescription(),
                    label.getScore());
        }
        if (labels.isEmpty()) {
            out.println("\tNo labels found.");
        }
    }

    private static byte[] toByte(URL url) throws IOException {
        InputStream in = new BufferedInputStream(url.openStream());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int n = 0;
        while (-1 != (n = in.read(buf))) {
            out.write(buf, 0, n);
        }
        out.close();
        in.close();
        return out.toByteArray();
    }

    private static ImageTagJsonModel getResultModel(AnnotateImageResponse response, Integer id) {
        if (response.getLabelAnnotations() == null) {
            return new ImageTagJsonModel(id, null,
                    response.getError() != null
                            ? new TagError(ImageTagErrorType.ApiError.getValue(), response.getError().getMessage())
                            : new TagError(ImageTagErrorType.UnknownError.getValue(), "Unknown error getting image annotations"));
        }
        return new ImageTagJsonModel(id,
                response.getLabelAnnotations()
                        .stream()
                        .map(e -> new TagModel(e.getConfidence(), e.getDescription(), e.getMid(), e.getScore(), e.getTopicality()))
                        .collect(Collectors.toList()), null
        );
    }

    private static ImageTagJsonModel getResultModel(String error, Integer id) {
        return new ImageTagJsonModel(id, null, new TagError(ImageTagErrorType.IOError.getValue(), error));
    }
}
