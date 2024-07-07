package com.tacz.guns.api.client.animation.gltf;

import com.tacz.guns.api.client.animation.gltf.accessor.AccessorData;
import com.tacz.guns.api.client.animation.gltf.accessor.AccessorDatas;
import com.tacz.guns.api.client.animation.gltf.accessor.AccessorSparseUtils;
import com.tacz.guns.client.resource.pojo.animation.gltf.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class AnimationStructure {
    private final List<AccessorModel> accessorModels = new ArrayList<>();
    private final List<AnimationModel> animationModels = new ArrayList<>();
    private final List<BufferModel> bufferModels = new ArrayList<>();
    private final List<BufferViewModel> bufferViewModels = new ArrayList<>();
    private final List<NodeModel> nodeModels = new ArrayList<>();
    private final RawAnimationStructure gltf;

    public AnimationStructure(RawAnimationStructure asset) {
        gltf = asset;
        createAccessorModels();
        createAnimationModels();
        createBufferModels();
        createBufferViewModels();
        createNodeModels();

        initBufferModels();
        initBufferViewModels();
        initAccessorModels();
        initAnimationModels();
        initNodeModels();
    }

    private static float[] clone(float[] array) {
        if (array == null) {
            return null;
        }
        return array.clone();
    }

    private static BufferViewModel createBufferViewModel(
            String uriString, ByteBuffer bufferData) {
        BufferModel bufferModel = new BufferModel();
        bufferModel.setUri(uriString);
        bufferModel.setBufferData(bufferData);

        BufferViewModel bufferViewModel =
                new BufferViewModel(null);
        bufferViewModel.setByteOffset(0);
        bufferViewModel.setByteLength(bufferData.capacity());
        bufferViewModel.setBufferModel(bufferModel);

        return bufferViewModel;
    }

    private static BufferViewModel createBufferViewModel(
            BufferView bufferView) {
        int byteOffset = bufferView.getByteOffset() == null ? 0 : bufferView.getByteOffset();
        int byteLength = bufferView.getByteLength();
        Integer byteStride = bufferView.getByteStride();
        Integer target = bufferView.getTarget();
        BufferViewModel bufferViewModel =
                new BufferViewModel(target);
        bufferViewModel.setByteOffset(byteOffset);
        bufferViewModel.setByteLength(byteLength);
        bufferViewModel.setByteStride(byteStride);
        return bufferViewModel;
    }

    private static boolean isDataUriString(String uriString) {
        if (uriString == null) {
            return false;
        }
        try {
            URI uri = new URI(uriString);
            return isDataUri(uri);
        } catch (URISyntaxException e) {
            return false;
        }
    }

    private static boolean isDataUri(URI uri) {
        return "data".equalsIgnoreCase(uri.getScheme());
    }

    public static byte[] readDataUri(String uriString) {
        String encoding = "base64,";
        int encodingIndex = uriString.indexOf(encoding);
        if (encodingIndex < 0) {
            throw new IllegalArgumentException(
                    "The given URI string is not a base64 encoded "
                            + "data URI string: " + uriString);
        }
        int contentStartIndex = encodingIndex + encoding.length();
        return Base64.getDecoder().decode(
                uriString.substring(contentStartIndex));
    }

    public List<BufferModel> getBufferModels() {
        return Collections.unmodifiableList(bufferModels);
    }

    public List<AccessorModel> getAccessorModels() {
        return accessorModels;
    }

    public List<AnimationModel> getAnimationModels() {
        return animationModels;
    }

    public List<BufferViewModel> getBufferViewModels() {
        return bufferViewModels;
    }

    public List<NodeModel> getNodeModels() {
        return nodeModels;
    }

    private void createBufferModels() {
        List<Buffer> buffers = gltf.getBuffers() == null ? Collections.emptyList() : gltf.getBuffers();
        for (int i = 0; i < buffers.size(); i++) {
            Buffer buffer = buffers.get(i);
            BufferModel bufferModel = new BufferModel();
            bufferModel.setUri(buffer.getUri());
            bufferModels.add(bufferModel);
        }
    }

    private void initBufferModels() {
        List<Buffer> buffers = gltf.getBuffers() == null ? Collections.emptyList() : gltf.getBuffers();
        for (int i = 0; i < buffers.size(); i++) {
            Buffer buffer = buffers.get(i);
            BufferModel bufferModel = bufferModels.get(i);
            {
                String uri = buffer.getUri();
                if (isDataUriString(uri)) {
                    byte[] data = readDataUri(uri);
                    ByteBuffer bufferData = Buffers.create(data);
                    bufferModel.setBufferData(bufferData);
                }
            }
        }
    }

    private void createAccessorModels() {
        List<Accessor> accessors = gltf.getAccessors() == null ? Collections.emptyList() : gltf.getAccessors();
        for (Accessor accessor : accessors) {
            Integer componentType = accessor.getComponentType();
            Integer count = accessor.getCount();
            ElementType elementType = ElementType.forString(accessor.getType());
            AccessorModel accessorModel = new AccessorModel(
                    componentType, count, elementType);
            accessorModels.add(accessorModel);
        }
    }

    private void initAccessorModels() {
        List<Accessor> accessors = gltf.getAccessors() == null ? Collections.emptyList() : gltf.getAccessors();
        for (int i = 0; i < accessors.size(); i++) {
            Accessor accessor = accessors.get(i);
            AccessorModel accessorModel = accessorModels.get(i);

            int byteOffset = accessor.getByteOffset() == null ? 0 : accessor.getByteOffset();
            accessorModel.setByteOffset(byteOffset);

            AccessorSparse accessorSparse = accessor.getSparse();
            if (accessorSparse == null) {
                initDenseAccessorModel(i, accessor, accessorModel);
            } else {
                initSparseAccessorModel(i, accessor, accessorModel);
            }
        }
    }

    private void createAnimationModels() {
        List<Animation> animations = gltf.getAnimations() == null ? Collections.emptyList() : gltf.getAnimations();
        for (int i = 0; i < animations.size(); i++) {
            animationModels.add(new AnimationModel());
        }
    }

    private void initAnimationModels() {
        List<Animation> animations = gltf.getAnimations() == null ? Collections.emptyList() : gltf.getAnimations();
        ;
        for (int i = 0; i < animations.size(); i++) {
            Animation animation = animations.get(i);
            AnimationModel animationModel = animationModels.get(i);
            animationModel.setName(animation.getName());
            List<AnimationChannel> channels =
                    animation.getChannels();
            for (AnimationChannel animationChannel : channels) {
                AnimationModel.Channel channel = createChannel(animation, animationChannel);
                animationModel.addChannel(channel);
            }
        }
    }

    private void createBufferViewModels() {
        List<BufferView> bufferViews = gltf.getBufferViews() == null ? Collections.emptyList() : gltf.getBufferViews();
        ;
        for (BufferView bufferView : bufferViews) {
            BufferViewModel bufferViewModel =
                    createBufferViewModel(bufferView);
            bufferViewModels.add(bufferViewModel);
        }
    }

    private void initBufferViewModels() {
        List<BufferView> bufferViews = gltf.getBufferViews() == null ? Collections.emptyList() : gltf.getBufferViews();
        ;
        for (int i = 0; i < bufferViews.size(); i++) {
            BufferView bufferView = bufferViews.get(i);

            BufferViewModel bufferViewModel = bufferViewModels.get(i);

            int bufferIndex = bufferView.getBuffer();
            BufferModel bufferModel = bufferModels.get(bufferIndex);
            bufferViewModel.setBufferModel(bufferModel);
        }
    }

    private void createNodeModels() {
        List<Node> nodes = gltf.getNodes() == null ? Collections.emptyList() : gltf.getNodes();
        for (int i = 0; i < nodes.size(); i++) {
            nodeModels.add(new NodeModel());
        }
    }

    private void initNodeModels() {
        List<Node> nodes = gltf.getNodes() == null ? Collections.emptyList() : gltf.getNodes();
        for (int i = 0; i < nodes.size(); i++) {
            Node node = nodes.get(i);

            NodeModel nodeModel = nodeModels.get(i);
            nodeModel.setName(node.getName());

            List<Integer> childIndices = node.getChildren() == null ? Collections.emptyList() : node.getChildren();
            ;
            for (Integer childIndex : childIndices) {
                NodeModel child = nodeModels.get(childIndex);
                nodeModel.addChild(child);
            }

            float[] matrix = node.getMatrix();
            float[] translation = node.getTranslation();
            float[] rotation = node.getRotation();
            float[] scale = node.getScale();
            nodeModel.setMatrix(clone(matrix));
            nodeModel.setTranslation(clone(translation));
            nodeModel.setRotation(clone(rotation));
            nodeModel.setScale(clone(scale));
        }
    }

    private AnimationModel.Channel createChannel(
            Animation animation, AnimationChannel animationChannel) {
        List<AnimationSampler> samplers = animation.getSamplers();

        int samplerIndex = animationChannel.getSampler();
        AnimationSampler animationSampler = samplers.get(samplerIndex);

        int inputAccessorIndex = animationSampler.getInput();
        AccessorModel inputAccessorModel =
                accessorModels.get(inputAccessorIndex);

        int outputAccessorIndex = animationSampler.getOutput();
        AccessorModel outputAccessorModel =
                accessorModels.get(outputAccessorIndex);

        String interpolationString =
                animationSampler.getInterpolation();
        AnimationModel.Interpolation interpolation =
                interpolationString == null ? AnimationModel.Interpolation.LINEAR :
                        AnimationModel.Interpolation.valueOf(interpolationString);

        AnimationModel.Sampler sampler = new AnimationModel.Sampler(
                inputAccessorModel, interpolation, outputAccessorModel);

        AnimationChannelTarget animationChannelTarget =
                animationChannel.getTarget();

        Integer nodeIndex = animationChannelTarget.getNode();
        NodeModel nodeModel = null;
        if (nodeIndex != null) {
            nodeModel = nodeModels.get(nodeIndex);
        }
        String path = animationChannelTarget.getPath();

        return new AnimationModel.Channel(sampler, nodeModel, path);
    }

    private void initDenseAccessorModel(int accessorIndex,
                                        Accessor accessor, AccessorModel accessorModel) {
        Integer bufferViewIndex = accessor.getBufferView();
        if (bufferViewIndex != null) {
            // When there is a BufferView referenced from the accessor, then
            // the corresponding BufferViewModel may be assigned directly
            BufferViewModel bufferViewModel =
                    bufferViewModels.get(bufferViewIndex);
            accessorModel.setBufferViewModel(bufferViewModel);
        } else {
            // When there is no BufferView referenced from the accessor,
            // then a NEW BufferViewModel (and Buffer) have to be created
            int count = accessorModel.getCount();
            int elementSizeInBytes = accessorModel.getElementSizeInBytes();
            int byteLength = elementSizeInBytes * count;
            ByteBuffer bufferData = Buffers.create(byteLength);
            String uriString = "buffer_for_accessor" + accessorIndex + ".bin";
            BufferViewModel bufferViewModel =
                    createBufferViewModel(uriString, bufferData);
            accessorModel.setBufferViewModel(bufferViewModel);
        }

        BufferViewModel bufferViewModel = accessorModel.getBufferViewModel();
        Integer byteStride = bufferViewModel.getByteStride();
        if (byteStride == null) {
            accessorModel.setByteStride(
                    accessorModel.getElementSizeInBytes());
        } else {
            accessorModel.setByteStride(byteStride);
        }
    }

    private void initSparseAccessorModel(int accessorIndex,
                                         Accessor accessor, AccessorModel accessorModel) {
        // When the (sparse!) Accessor already refers to a BufferView,
        // then this BufferView has to be replaced with a new one,
        // to which the data substitution will be applied
        int count = accessorModel.getCount();
        int elementSizeInBytes = accessorModel.getElementSizeInBytes();
        int byteLength = elementSizeInBytes * count;
        ByteBuffer bufferData = Buffers.create(byteLength);
        String uriString = "buffer_for_accessor" + accessorIndex + ".bin";
        BufferViewModel denseBufferViewModel =
                createBufferViewModel(uriString, bufferData);
        accessorModel.setBufferViewModel(denseBufferViewModel);
        accessorModel.setByteOffset(0);

        Integer bufferViewIndex = accessor.getBufferView();
        if (bufferViewIndex != null) {
            // If the accessor refers to a BufferView, then the corresponding
            // data serves as the basis for the initialization of the values,
            // before the sparse substitution is applied
            Consumer<ByteBuffer> sparseSubstitutionCallback = denseByteBuffer ->
            {
                BufferViewModel baseBufferViewModel =
                        bufferViewModels.get(bufferViewIndex);
                ByteBuffer baseBufferViewData =
                        baseBufferViewModel.getBufferViewData();
                AccessorData baseAccessorData = AccessorDatas.create(
                        accessorModel, baseBufferViewData);
                AccessorData denseAccessorData =
                        AccessorDatas.create(accessorModel, bufferData);
                substituteSparseAccessorData(accessor, accessorModel,
                        denseAccessorData, baseAccessorData);
            };
            denseBufferViewModel.setSparseSubstitutionCallback(
                    sparseSubstitutionCallback);
        } else {
            // When the sparse accessor does not yet refer to a BufferView,
            // then a new one is created,
            Consumer<ByteBuffer> sparseSubstitutionCallback = denseByteBuffer ->
            {
                AccessorData denseAccessorData =
                        AccessorDatas.create(accessorModel, bufferData);
                substituteSparseAccessorData(accessor, accessorModel,
                        denseAccessorData, null);
            };
            denseBufferViewModel.setSparseSubstitutionCallback(
                    sparseSubstitutionCallback);
        }
    }

    private void substituteSparseAccessorData(
            Accessor accessor, AccessorModel accessorModel,
            AccessorData denseAccessorData, AccessorData baseAccessorData) {
        AccessorSparse accessorSparse = accessor.getSparse();
        int count = accessorSparse.getCount();

        AccessorSparseIndices accessorSparseIndices =
                accessorSparse.getIndices();
        AccessorData sparseIndicesAccessorData =
                createSparseIndicesAccessorData(accessorSparseIndices, count);

        AccessorSparseValues accessorSparseValues = accessorSparse.getValues();
        ElementType elementType = accessorModel.getElementType();
        AccessorData sparseValuesAccessorData =
                createSparseValuesAccessorData(accessorSparseValues,
                        accessorModel.getComponentType(),
                        elementType.getNumComponents(), count);

        AccessorSparseUtils.substituteAccessorData(
                denseAccessorData,
                baseAccessorData,
                sparseIndicesAccessorData,
                sparseValuesAccessorData);
    }

    private AccessorData createSparseIndicesAccessorData(
            AccessorSparseIndices accessorSparseIndices, int count) {
        Integer componentType = accessorSparseIndices.getComponentType();
        Integer bufferViewIndex = accessorSparseIndices.getBufferView();
        BufferViewModel bufferViewModel = bufferViewModels.get(bufferViewIndex);
        ByteBuffer bufferViewData = bufferViewModel.getBufferViewData();
        int byteOffset = accessorSparseIndices.getByteOffset() == null ? 0 : accessorSparseIndices.getByteOffset();
        return AccessorDatas.create(
                componentType, bufferViewData, byteOffset, count, 1, null);
    }

    private AccessorData createSparseValuesAccessorData(
            AccessorSparseValues accessorSparseValues,
            int componentType, int numComponentsPerElement, int count) {
        Integer bufferViewIndex = accessorSparseValues.getBufferView();
        BufferViewModel bufferViewModel = bufferViewModels.get(bufferViewIndex);
        ByteBuffer bufferViewData = bufferViewModel.getBufferViewData();
        int byteOffset = accessorSparseValues.getByteOffset() == null ? 0 : accessorSparseValues.getByteOffset();
        return AccessorDatas.create(
                componentType, bufferViewData, byteOffset, count,
                numComponentsPerElement, null);
    }
}
