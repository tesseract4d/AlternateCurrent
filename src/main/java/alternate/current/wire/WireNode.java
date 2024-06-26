package alternate.current.wire;

import alternate.current.util.BlockPos;
import alternate.current.util.BlockState;
import alternate.current.util.Redstone;
import net.minecraft.init.Blocks;
import net.minecraft.util.MathHelper;
import net.minecraft.world.WorldServer;

/**
 * A WireNode is a Node that represents a wire in the world. It stores all the
 * information about the wire that the WireHandler needs to calculate power
 * changes.
 *
 * @author Space Walker
 */
public class WireNode extends Node {

	final WireConnectionManager connections;

	/** The power level this wire currently holds in the world. */
	int currentPower;
	/**
	 * While calculating power changes for a network, this field is used to keep
	 * track of the power level this wire should have.
	 */
	int virtualPower;
	/** The power level received from non-wire components. */
	int externalPower;
	/**
	 * A 4-bit number that keeps track of the power flow of the wires that give this
	 * wire its power level.
	 */
	int flowIn;
	/** The direction of power flow, based on the incoming flow. */
	int iFlowDir;
	boolean added;
	boolean removed;
	boolean shouldBreak;
	boolean root;
	boolean discovered;
	boolean searched;

	/** The next wire in the simple queue. */
	WireNode next_wire;

	WireNode(WorldServer world, BlockPos pos, BlockState state) {
		super(world);

		this.pos = pos;
		this.state = state;

		this.connections = new WireConnectionManager(this);

		this.virtualPower = this.currentPower = this.state.get();
		this.priority = priority();
	}

	@Override
	Node set(BlockPos pos, BlockState state, boolean clearNeighbors) {
		throw new UnsupportedOperationException("Cannot update a WireNode!");
	}

	@Override
	int priority() {
		return MathHelper.clamp_int(virtualPower, Redstone.SIGNAL_MIN, Redstone.SIGNAL_MAX);
	}

	@Override
	public boolean isWire() {
		return true;
	}

	@Override
	public WireNode asWire() {
		return this;
	}

	boolean offerPower(int power, int iDir) {
		if (removed || shouldBreak) {
			return false;
		}
		if (power == virtualPower) {
			flowIn |= (1 << iDir);
			return false;
		}
		if (power > virtualPower) {
			virtualPower = power;
			flowIn = (1 << iDir);

			return true;
		}

		return false;
	}

	boolean setPower() {
		if (removed) {
			return true;
		}

		state = WorldHelper.getBlockState(world, pos);

		if (!state.is(Blocks.redstone_wire)) {
			return false; // we should never get here
		}

		if (shouldBreak) {
			state.dropAsItem(world, pos);
			world.setBlockToAir(pos.x, pos.y, pos.z);

			return true;
		}

		currentPower = MathHelper.clamp_int(virtualPower, Redstone.SIGNAL_MIN, Redstone.SIGNAL_MAX);
		state = state.with(currentPower);

		return WorldHelper.setWireState(world, pos, state);
	}
}
