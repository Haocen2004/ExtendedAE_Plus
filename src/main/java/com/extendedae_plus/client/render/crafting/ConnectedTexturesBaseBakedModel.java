package com.extendedae_plus.client.render.crafting;

import com.google.common.collect.Lists;
import com.mojang.math.Vector3f;
import it.unimi.dsi.fastutil.objects.Object2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.IDynamicBakedModel;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import net.minecraftforge.client.model.pipeline.QuadBakingVertexConsumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;

public abstract class ConnectedTexturesBaseBakedModel implements IDynamicBakedModel {

    private static final Object2ReferenceMap<FaceCorner, List<Vector3f>> V_MAP = createVertexMap();
    private static final EnumMap<Direction, List<Vector3f>> F_MAP = createFaceMap();
    private static final ModelProperty<Connect> CONNECT_STATE = new ModelProperty<>();

    private static final int LU = 0;
    private static final int RU = 1;
    private static final int LD = 2;
    private static final int RD = 4;

    private final ChunkRenderTypeSet renderTypes;
    private final TextureAtlasSprite face;
    private final TextureAtlasSprite sides;
    private final TextureAtlasSprite poweredSides;

    private HashMap<Direction, TextureAtlasSprite> faceAnimations;

    private boolean renderOppositeSide = false;
    private RenderType faceRenderType;
    private RenderType sideRenderType;

    private boolean faceEmissive = false;
    private boolean sideEmissive = false;
    private boolean faceAnimationEmissive = false;

    protected ConnectedTexturesBaseBakedModel(
            RenderType renderType, TextureAtlasSprite face, TextureAtlasSprite sides, TextureAtlasSprite poweredSides) {
        this(ChunkRenderTypeSet.of(renderType), face, sides, poweredSides);
        this.faceRenderType = renderType;
        this.sideRenderType = renderType;
    }

    protected ConnectedTexturesBaseBakedModel(
            RenderType faceRenderType,
            RenderType sideRenderType,
            TextureAtlasSprite face,
            TextureAtlasSprite sides,
            TextureAtlasSprite poweredSides) {
        this(ChunkRenderTypeSet.of(faceRenderType, sideRenderType), face, sides, poweredSides);
        this.faceRenderType = faceRenderType;
        this.sideRenderType = sideRenderType;
    }

    private ConnectedTexturesBaseBakedModel(
            ChunkRenderTypeSet renderTypes,
            TextureAtlasSprite face,
            TextureAtlasSprite sides,
            TextureAtlasSprite poweredSides) {
        this.renderTypes = renderTypes;
        this.face = face;
        this.sides = sides;
        this.poweredSides = poweredSides;
    }

    protected void setFaceEmissive(boolean faceEmissive) {
        this.faceEmissive = faceEmissive;
    }

    protected void setSideEmissive(boolean sideEmissive) {
        this.sideEmissive = sideEmissive;
    }

    protected void setFaceAnimation(HashMap<Direction, TextureAtlasSprite> faceAnimations, boolean emissive) {
        this.faceAnimations = faceAnimations;
        this.faceAnimationEmissive = emissive;
    }

    protected void setRenderOppositeSide(boolean renderOppositeSide) {
        this.renderOppositeSide = renderOppositeSide;
    }

    protected abstract boolean shouldConnect(Block block);

    protected abstract boolean shouldBeEmissive(BlockState state);

    @Override
    public @NotNull ModelData getModelData(
            @NotNull BlockAndTintGetter world,
            @NotNull BlockPos pos,
            @NotNull BlockState state,
            @NotNull ModelData modelData) {
        var connect = new Connect();
        connect.init(pos);

        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    var offset = pos.offset(x, y, z);
                    var block = world.getBlockState(offset)
                            .getAppearance(world, offset, Direction.NORTH, state, pos)
                            .getBlock();
                    if (shouldConnect(block)) {
                        connect.set(x, y, z);
                    }
                }
            }
        }

        return modelData.derive().with(CONNECT_STATE, connect).build();
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(
            @Nullable BlockState blockState,
            @Nullable Direction side,
            @NotNull RandomSource randomSource,
            @NotNull ModelData modelData,
            @Nullable RenderType renderType) {
        if (side == null) {
            return Collections.emptyList();
        }

        var connect = modelData.get(CONNECT_STATE);
        if (connect == null) {
            return Collections.emptyList();
        }

        var powered = blockState != null && shouldBeEmissive(blockState);
        List<BakedQuad> quads = new ArrayList<>();

        if (renderType == null || renderTypes.contains(renderType)) {
            if (renderType == null || renderType == this.faceRenderType) {
                addFaceQuad(quads, side, connect.getFace(side), powered);
            }

            if (this.sides != null && (renderType == null || renderType == this.sideRenderType)) {
                addSides(quads, connect, side, powered);
                if (this.renderOppositeSide) {
                    addSides(quads, connect, side.getOpposite(), powered, true);
                }
            }
        }

        return quads;
    }

    private void addSides(List<BakedQuad> quads, Connect connect, Direction side, boolean powered) {
        addSides(quads, connect, side, powered, false);
    }

    private void addSides(List<BakedQuad> quads, Connect connect, Direction side, boolean powered, boolean renderOpposite) {
        addSideQuad(quads, side, connect.getIndex(side, LU), LU, powered, renderOpposite);
        addSideQuad(quads, side, connect.getIndex(side, RU), RU, powered, renderOpposite);
        addSideQuad(quads, side, connect.getIndex(side, LD), LD, powered, renderOpposite);
        addSideQuad(quads, side, connect.getIndex(side, RD), RD, powered, renderOpposite);
    }

    private List<Vector3f> calculateCorners(Direction face, int corner) {
        return V_MAP.get(new FaceCorner(face, corner));
    }

    private void addFaceQuad(List<BakedQuad> quads, Direction side, int index, boolean powered) {
        if (index < 0) {
            return;
        }

        var cons = F_MAP.get(side);
        var normal = side.getNormal();
        var step = getNormalStep(normal);
        var c1 = cons.get(0).copy();
        var c2 = cons.get(1).copy();
        var c3 = cons.get(2).copy();
        var c4 = cons.get(3).copy();
        c1.sub(step);
        c2.sub(step);
        c3.sub(step);
        c4.sub(step);

        var builder = new QuadBakingVertexConsumer(quads::add);
        builder.setSprite(this.face);
        builder.setDirection(side);
        builder.setShade(true);

        boolean emissive = this.faceEmissive && powered;
        putVertex(builder, this.face, normal, c1.x(), c1.y(), c1.z(), 0, 0, emissive);
        putVertex(builder, this.face, normal, c2.x(), c2.y(), c2.z(), 0, 1, emissive);
        putVertex(builder, this.face, normal, c3.x(), c3.y(), c3.z(), 1, 1, emissive);
        putVertex(builder, this.face, normal, c4.x(), c4.y(), c4.z(), 1, 0, emissive);

        if (powered && this.faceAnimations != null && this.faceAnimations.get(side) != null) {
            var animation = this.faceAnimations.get(side);
            var animationBuilder = new QuadBakingVertexConsumer(quads::add);
            animationBuilder.setSprite(animation);
            animationBuilder.setDirection(side);
            animationBuilder.setShade(true);

            boolean animationEmissive = this.faceAnimationEmissive;
            putVertex(animationBuilder, animation, normal, c1.x(), c1.y(), c1.z(), 0, 0, animationEmissive);
            putVertex(animationBuilder, animation, normal, c2.x(), c2.y(), c2.z(), 0, 1, animationEmissive);
            putVertex(animationBuilder, animation, normal, c3.x(), c3.y(), c3.z(), 1, 1, animationEmissive);
            putVertex(animationBuilder, animation, normal, c4.x(), c4.y(), c4.z(), 1, 0, animationEmissive);
        }
    }

    private void addSideQuad(
            List<BakedQuad> quads,
            Direction side,
            int index,
            int corner,
            boolean powered,
            boolean renderOpposite) {
        if (index < 0) {
            return;
        }

        var cons = calculateCorners(side, corner);
        var sprite = powered ? this.poweredSides : this.sides;
        var builder = new QuadBakingVertexConsumer(quads::add);
        builder.setSprite(sprite);
        builder.setDirection(side);
        builder.setShade(true);

        var normal = side.getNormal();
        var c1 = renderOpposite ? cons.get(3).copy() : cons.get(0).copy();
        var c2 = renderOpposite ? cons.get(2).copy() : cons.get(1).copy();
        var c3 = renderOpposite ? cons.get(1).copy() : cons.get(2).copy();
        var c4 = renderOpposite ? cons.get(0).copy() : cons.get(3).copy();

        if (renderOpposite) {
            var step = getNormalStep(normal, 2);
            c1.sub(step);
            c2.sub(step);
            c3.sub(step);
            c4.sub(step);
        }

        float u0 = renderOpposite ? getU1(index) : getU0(index);
        float u1 = renderOpposite ? getU0(index) : getU1(index);
        float v0 = getV0(index);
        float v1 = getV1(index);
        boolean emissive = this.sideEmissive && powered;

        switch (corner) {
            case LU -> {
                putVertex(builder, sprite, normal, c1.x(), c1.y(), c1.z(), u0, v0, emissive);
                putVertex(builder, sprite, normal, c2.x(), c2.y(), c2.z(), u0, v1, emissive);
                putVertex(builder, sprite, normal, c3.x(), c3.y(), c3.z(), u1, v1, emissive);
                putVertex(builder, sprite, normal, c4.x(), c4.y(), c4.z(), u1, v0, emissive);
            }
            case RU -> {
                putVertex(builder, sprite, normal, c1.x(), c1.y(), c1.z(), u1, v0, emissive);
                putVertex(builder, sprite, normal, c2.x(), c2.y(), c2.z(), u1, v1, emissive);
                putVertex(builder, sprite, normal, c3.x(), c3.y(), c3.z(), u0, v1, emissive);
                putVertex(builder, sprite, normal, c4.x(), c4.y(), c4.z(), u0, v0, emissive);
            }
            case LD -> {
                putVertex(builder, sprite, normal, c1.x(), c1.y(), c1.z(), u0, v1, emissive);
                putVertex(builder, sprite, normal, c2.x(), c2.y(), c2.z(), u0, v0, emissive);
                putVertex(builder, sprite, normal, c3.x(), c3.y(), c3.z(), u1, v0, emissive);
                putVertex(builder, sprite, normal, c4.x(), c4.y(), c4.z(), u1, v1, emissive);
            }
            case RD -> {
                putVertex(builder, sprite, normal, c1.x(), c1.y(), c1.z(), u1, v1, emissive);
                putVertex(builder, sprite, normal, c2.x(), c2.y(), c2.z(), u1, v0, emissive);
                putVertex(builder, sprite, normal, c3.x(), c3.y(), c3.z(), u0, v0, emissive);
                putVertex(builder, sprite, normal, c4.x(), c4.y(), c4.z(), u0, v1, emissive);
            }
        }
    }

    private static EnumMap<Direction, List<Vector3f>> createFaceMap() {
        EnumMap<Direction, List<Vector3f>> map = new EnumMap<>(Direction.class);
        map.put(Direction.EAST, List.of(new Vector3f(1, 1, 1), new Vector3f(1, 0, 1), new Vector3f(1, 0, 0), new Vector3f(1, 1, 0)));
        map.put(Direction.WEST, Lists.reverse(List.of(new Vector3f(0, 1, 1), new Vector3f(0, 0, 1), new Vector3f(0, 0, 0), new Vector3f(0, 1, 0))));
        map.put(Direction.UP, List.of(new Vector3f(1, 1, 1), new Vector3f(1, 1, 0), new Vector3f(0, 1, 0), new Vector3f(0, 1, 1)));
        map.put(Direction.DOWN, Lists.reverse(List.of(new Vector3f(1, 0, 1), new Vector3f(1, 0, 0), new Vector3f(0, 0, 0), new Vector3f(0, 0, 1))));
        map.put(Direction.SOUTH, List.of(new Vector3f(0, 1, 1), new Vector3f(0, 0, 1), new Vector3f(1, 0, 1), new Vector3f(1, 1, 1)));
        map.put(Direction.NORTH, Lists.reverse(List.of(new Vector3f(0, 1, 0), new Vector3f(0, 0, 0), new Vector3f(1, 0, 0), new Vector3f(1, 1, 0))));
        return map;
    }

    private static Object2ReferenceMap<FaceCorner, List<Vector3f>> createVertexMap() {
        Object2ReferenceMap<FaceCorner, List<Vector3f>> map = new Object2ReferenceOpenHashMap<>();
        map.put(new FaceCorner(Direction.EAST, LU), List.of(new Vector3f(1, 1, 1), new Vector3f(1, 0.5f, 1), new Vector3f(1, 0.5f, 0.5f), new Vector3f(1, 1, 0.5f)));
        map.put(new FaceCorner(Direction.EAST, RU), List.of(new Vector3f(1, 1, 0.5f), new Vector3f(1, 0.5f, 0.5f), new Vector3f(1, 0.5f, 0), new Vector3f(1, 1, 0)));
        map.put(new FaceCorner(Direction.EAST, LD), List.of(new Vector3f(1, 0.5f, 1), new Vector3f(1, 0, 1), new Vector3f(1, 0, 0.5f), new Vector3f(1, 0.5f, 0.5f)));
        map.put(new FaceCorner(Direction.EAST, RD), List.of(new Vector3f(1, 0.5f, 0.5f), new Vector3f(1, 0, 0.5f), new Vector3f(1, 0, 0), new Vector3f(1, 0.5f, 0)));
        map.put(new FaceCorner(Direction.WEST, LU), List.of(new Vector3f(0, 1, 0), new Vector3f(0, 0.5f, 0), new Vector3f(0, 0.5f, 0.5f), new Vector3f(0, 1, 0.5f)));
        map.put(new FaceCorner(Direction.WEST, RU), List.of(new Vector3f(0, 1, 0.5f), new Vector3f(0, 0.5f, 0.5f), new Vector3f(0, 0.5f, 1), new Vector3f(0, 1, 1)));
        map.put(new FaceCorner(Direction.WEST, LD), List.of(new Vector3f(0, 0.5f, 0), new Vector3f(0, 0, 0), new Vector3f(0, 0, 0.5f), new Vector3f(0, 0.5f, 0.5f)));
        map.put(new FaceCorner(Direction.WEST, RD), List.of(new Vector3f(0, 0.5f, 0.5f), new Vector3f(0, 0, 0.5f), new Vector3f(0, 0, 1), new Vector3f(0, 0.5f, 1)));
        map.put(new FaceCorner(Direction.SOUTH, LU), List.of(new Vector3f(0, 1, 1), new Vector3f(0, 0.5f, 1), new Vector3f(0.5f, 0.5f, 1), new Vector3f(0.5f, 1, 1)));
        map.put(new FaceCorner(Direction.SOUTH, RU), List.of(new Vector3f(0.5f, 1, 1), new Vector3f(0.5f, 0.5f, 1), new Vector3f(1, 0.5f, 1), new Vector3f(1, 1, 1)));
        map.put(new FaceCorner(Direction.SOUTH, LD), List.of(new Vector3f(0, 0.5f, 1), new Vector3f(0, 0, 1), new Vector3f(0.5f, 0, 1), new Vector3f(0.5f, 0.5f, 1)));
        map.put(new FaceCorner(Direction.SOUTH, RD), List.of(new Vector3f(0.5f, 0.5f, 1), new Vector3f(0.5f, 0, 1), new Vector3f(1, 0, 1), new Vector3f(1, 0.5f, 1)));
        map.put(new FaceCorner(Direction.NORTH, LU), List.of(new Vector3f(1, 1, 0), new Vector3f(1, 0.5f, 0), new Vector3f(0.5f, 0.5f, 0), new Vector3f(0.5f, 1, 0)));
        map.put(new FaceCorner(Direction.NORTH, RU), List.of(new Vector3f(0.5f, 1, 0), new Vector3f(0.5f, 0.5f, 0), new Vector3f(0, 0.5f, 0), new Vector3f(0, 1, 0)));
        map.put(new FaceCorner(Direction.NORTH, LD), List.of(new Vector3f(1, 0.5f, 0), new Vector3f(1, 0, 0), new Vector3f(0.5f, 0, 0), new Vector3f(0.5f, 0.5f, 0)));
        map.put(new FaceCorner(Direction.NORTH, RD), List.of(new Vector3f(0.5f, 0.5f, 0), new Vector3f(0.5f, 0, 0), new Vector3f(0, 0, 0), new Vector3f(0, 0.5f, 0)));
        map.put(new FaceCorner(Direction.UP, LU), List.of(new Vector3f(0, 1, 1), new Vector3f(0.5f, 1, 1), new Vector3f(0.5f, 1, 0.5f), new Vector3f(0, 1, 0.5f)));
        map.put(new FaceCorner(Direction.UP, RU), List.of(new Vector3f(0, 1, 0.5f), new Vector3f(0.5f, 1, 0.5f), new Vector3f(0.5f, 1, 0), new Vector3f(0, 1, 0)));
        map.put(new FaceCorner(Direction.UP, LD), List.of(new Vector3f(0.5f, 1, 1), new Vector3f(1, 1, 1), new Vector3f(1, 1, 0.5f), new Vector3f(0.5f, 1, 0.5f)));
        map.put(new FaceCorner(Direction.UP, RD), List.of(new Vector3f(0.5f, 1, 0.5f), new Vector3f(1, 1, 0.5f), new Vector3f(1, 1, 0), new Vector3f(0.5f, 1, 0)));
        map.put(new FaceCorner(Direction.DOWN, LU), List.of(new Vector3f(1, 0, 1), new Vector3f(0.5f, 0, 1), new Vector3f(0.5f, 0, 0.5f), new Vector3f(1, 0, 0.5f)));
        map.put(new FaceCorner(Direction.DOWN, RU), List.of(new Vector3f(1, 0, 0.5f), new Vector3f(0.5f, 0, 0.5f), new Vector3f(0.5f, 0, 0), new Vector3f(1, 0, 0)));
        map.put(new FaceCorner(Direction.DOWN, LD), List.of(new Vector3f(0.5f, 0, 1), new Vector3f(0, 0, 1), new Vector3f(0, 0, 0.5f), new Vector3f(0.5f, 0, 0.5f)));
        map.put(new FaceCorner(Direction.DOWN, RD), List.of(new Vector3f(0.5f, 0, 0.5f), new Vector3f(0, 0, 0.5f), new Vector3f(0, 0, 0), new Vector3f(0.5f, 0, 0)));
        return map;
    }

    private void putVertex(
            QuadBakingVertexConsumer builder,
            TextureAtlasSprite sprite,
            Vec3i normal,
            float x,
            float y,
            float z,
            float u,
            float v,
            boolean emissive) {
        float au = sprite.getU(u * 16f);
        float av = sprite.getV(v * 16f);
        int light = emissive ? 240 : 0;
        builder.vertex(x, y, z)
                .color(255, 255, 255, 255)
                .uv(au, av)
                .uv2(light, light)
                .normal(normal.getX(), normal.getY(), normal.getZ())
                .endVertex();
    }

    private float getU0(int index) {
        return switch (index) {
            case 1, 3 -> 0.5f;
            default -> 0f;
        };
    }

    private float getU1(int index) {
        return switch (index) {
            case 1, 3 -> 1f;
            default -> 0.5f;
        };
    }

    private float getV0(int index) {
        return switch (index) {
            case 2, 3 -> 0.5f;
            default -> 0f;
        };
    }

    private float getV1(int index) {
        return switch (index) {
            case 2, 3 -> 1f;
            default -> 0.5f;
        };
    }

    @Override
    public boolean useAmbientOcclusion() {
        return false;
    }

    @Override
    public boolean isGui3d() {
        return false;
    }

    @Override
    public boolean usesBlockLight() {
        return false;
    }

    @Override
    public boolean isCustomRenderer() {
        return false;
    }

    @Override
    public @NotNull TextureAtlasSprite getParticleIcon() {
        return this.sides;
    }

    @Override
    public @NotNull ItemOverrides getOverrides() {
        return ItemOverrides.EMPTY;
    }

    @ParametersAreNonnullByDefault
    @Override
    public @NotNull ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
        return renderTypes;
    }

    private Vector3f getNormalStep(Vec3i normal) {
        return getNormalStep(normal, 1f);
    }

    private Vector3f getNormalStep(Vec3i normal, float multiplier) {
        return new Vector3f(
                getNormalStep(normal.getX(), multiplier),
                getNormalStep(normal.getY(), multiplier),
                getNormalStep(normal.getZ(), multiplier));
    }

    private float getNormalStep(int step, float multiplier) {
        return multiplier * (step > 0 ? 0.002f : step < 0 ? -0.002f : 0f);
    }

    private static class Connect {

        private final boolean[][][] connects = new boolean[3][3][3];
        private int face;

        int getFace(Direction faceDirection) {
            if (blocked(faceDirection)) {
                return -1;
            }
            return this.face;
        }

        void init(BlockPos pos) {
            this.face = Math.abs((pos.getX() ^ pos.getY() ^ pos.getZ()) % 3);
        }

        void set(int x, int y, int z) {
            this.connects[x + 1][y + 1][z + 1] = true;
        }

        int getIndex(Direction faceDirection, int corner) {
            if (blocked(faceDirection)) {
                return -1;
            }

            return switch (faceDirection) {
                case WEST, EAST -> getIndexX(faceDirection, corner);
                case DOWN, UP -> getIndexY(faceDirection, corner);
                case NORTH, SOUTH -> getIndexZ(faceDirection, corner);
            };
        }

        boolean blocked(Direction faceDirection) {
            var pos = faceDirection.getNormal().offset(1, 1, 1);
            return this.connects[pos.getX()][pos.getY()][pos.getZ()];
        }

        int getIndexX(Direction faceDirection, int corner) {
            int x = faceDirection.getStepX();
            return switch (corner) {
                case LU -> getIndex(this.connects[1][1][1 + x], this.connects[1][2][1], this.connects[1][2][1 + x]);
                case RU -> getIndex(this.connects[1][1][1 - x], this.connects[1][2][1], this.connects[1][2][1 - x]);
                case LD -> getIndex(this.connects[1][1][1 + x], this.connects[1][0][1], this.connects[1][0][1 + x]);
                case RD -> getIndex(this.connects[1][1][1 - x], this.connects[1][0][1], this.connects[1][0][1 - x]);
                default -> -1;
            };
        }

        int getIndexZ(Direction faceDirection, int corner) {
            int z = faceDirection.getStepZ();
            return switch (corner) {
                case LU -> getIndex(this.connects[1 - z][1][1], this.connects[1][2][1], this.connects[1 - z][2][1]);
                case RU -> getIndex(this.connects[1 + z][1][1], this.connects[1][2][1], this.connects[1 + z][2][1]);
                case LD -> getIndex(this.connects[1 - z][1][1], this.connects[1][0][1], this.connects[1 - z][0][1]);
                case RD -> getIndex(this.connects[1 + z][1][1], this.connects[1][0][1], this.connects[1 + z][0][1]);
                default -> -1;
            };
        }

        int getIndexY(Direction faceDirection, int corner) {
            int y = faceDirection.getStepY();
            return switch (corner) {
                case LU -> getIndex(this.connects[1][1][2], this.connects[1 - y][1][1], this.connects[1 - y][1][2]);
                case RU -> getIndex(this.connects[1][1][0], this.connects[1 - y][1][1], this.connects[1 - y][1][0]);
                case LD -> getIndex(this.connects[1][1][2], this.connects[1 + y][1][1], this.connects[1 + y][1][2]);
                case RD -> getIndex(this.connects[1][1][0], this.connects[1 + y][1][1], this.connects[1 + y][1][0]);
                default -> -1;
            };
        }

        @SuppressWarnings("ConstantValue")
        int getIndex(boolean a, boolean b, boolean c) {
            if (!a && !b) {
                return 0;
            }
            if (a && b && !c) {
                return 1;
            }
            if (!a && b) {
                return 2;
            }
            if (a && !b) {
                return 3;
            }
            return -1;
        }
    }

    private record FaceCorner(Direction face, int corner) {}
}
