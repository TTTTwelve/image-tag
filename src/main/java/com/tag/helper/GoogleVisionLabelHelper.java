package com.tag.helper;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionScopes;
import com.google.api.services.vision.v1.model.*;
import com.google.common.collect.ImmutableList;
import com.tag.model.ImageTagJsonModel;
import com.tag.model.TagModel;

import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by jiaqiwu on 2019/6/17.
 */
public class GoogleVisionLabelHelper {

    private static final String APPLICATION_NAME = "Google-VisionLabelSample/1.0";

    private static final int MAX_LABELS = 20;

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

        AnnotateImageRequest request =
                new AnnotateImageRequest()
                        .setImage(new Image().setSource(new ImageSource().setImageUri(path)))
//                      .setImage(new Image().encodeContent(data))
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
        if (response.getLabelAnnotations() == null) {
            return new ImageTagJsonModel(
                    response.getError() != null
                            ? response.getError().getMessage()
                            : "Unknown error getting image annotations"
            );
        }

        return new ImageTagJsonModel(
                response.getLabelAnnotations()
                        .stream()
                        .map(e -> new TagModel(e.getConfidence(), e.getDescription(), e.getMid(), e.getScore(), e.getTopicality()))
                        .collect(Collectors.toList())
        );
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
}
