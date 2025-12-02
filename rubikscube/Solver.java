package rubikscube;

import java.io.File;
import java.lang.invoke.MethodHandles;

public class Solver {
	public static void main(String[] args) {
		// System.out.println("number of arguments: " + args.length);
		// for (int i = 0; i < args.length; i++) {
		// System.out.println(args[i]);
		// }

		if (args.length < 2) {
			System.out.println("File names are not specified");
			System.out.println(
					"usage: java " + MethodHandles.lookup().lookupClass().getName() + " input_file output_file");
			return;
		}

		try {
			File inputFile = new File(args[0]);
			File outputFile = new File(args[1]);


			cube cube = parseInput(inputFile);
			
			String solution = search.solve(cube);

			java.nio.file.Files.write(outputFile.toPath(), solution.getBytes());

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static cube parseInput(File file) throws java.io.IOException {
		java.util.List<String> lines = java.nio.file.Files.readAllLines(file.toPath());
		cube cube = new cube();

		for (int r = 0; r < 3; r++) {
			String line = lines.get(r);
			for (int c = 0; c < 3; c++) {
				cube.state[cube.UP][r * 3 + c] = line.charAt(c + 3);
			}
		}
		for (int r = 0; r < 3; r++) {
			String line = lines.get(r + 3);
			for (int c = 0; c < 3; c++) {
				cube.state[cube.LEFT][r * 3 + c] = line.charAt(c);
			}
			for (int c = 0; c < 3; c++) {
				cube.state[cube.FRONT][r * 3 + c] = line.charAt(c + 3);
			}
			for (int c = 0; c < 3; c++) {
				cube.state[cube.RIGHT][r * 3 + c] = line.charAt(c + 6);
			}
			for (int c = 0; c < 3; c++) {
				cube.state[cube.BACK][r * 3 + c] = line.charAt(c + 9);
			}
		}

		for (int r = 0; r < 3; r++) {
			String line = lines.get(r + 6);
			for (int c = 0; c < 3; c++) {
				cube.state[cube.DOWN][r * 3 + c] = line.charAt(c + 3);
			}
		}

		return cube;
	}
}
