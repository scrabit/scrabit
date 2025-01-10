package tictactoe.logic

import tictactoe.logic.GameLogic.Board
import tictactoe.logic.GameLogic.Cell
import tictactoe.logic.GameLogic.Mark

import org.scalatest.funsuite.AnyFunSuite

class BoardTest extends AnyFunSuite {

  test("Move on an empty cell should update the board") {
    val emptyBoard   = Board(Vector.fill(3)(Vector.fill(3)(Cell(None))))
    val updatedBoard = emptyBoard.move(Mark.X, 0, 0)

    assert(updatedBoard.isDefined)
    assert(updatedBoard.get.cells(0)(0).mark.contains(Mark.X))
  }

  test("Move on an occupied cell should return None") {
    val board = Board(
      Vector(
        Vector(Cell(Some(Mark.X)), Cell(None), Cell(None)),
        Vector(Cell(None), Cell(None), Cell(None)),
        Vector(Cell(None), Cell(None), Cell(None))
      )
    )

    assert(board.move(Mark.O, 0, 0).isEmpty)
  }

  test("Check if board is full") {
    val fullBoard = Board(
      Vector(
        Vector(Cell(Some(Mark.X)), Cell(Some(Mark.O)), Cell(Some(Mark.X))),
        Vector(Cell(Some(Mark.O)), Cell(Some(Mark.X)), Cell(Some(Mark.O))),
        Vector(Cell(Some(Mark.X)), Cell(Some(Mark.O)), Cell(Some(Mark.X)))
      )
    )

    assert(fullBoard.isFull)
  }

  test("Check if board is not full") {
    val notFullBoard = Board(
      Vector(
        Vector(Cell(Some(Mark.X)), Cell(Some(Mark.O)), Cell(None)),
        Vector(Cell(None), Cell(Some(Mark.X)), Cell(None)),
        Vector(Cell(Some(Mark.O)), Cell(None), Cell(Some(Mark.X)))
      )
    )

    assert(!notFullBoard.isFull)
  }

  test("Check winning condition for a row") {
    val board = Board(
      Vector(
        Vector(Cell(Some(Mark.X)), Cell(Some(Mark.X)), Cell(Some(Mark.X))),
        Vector(Cell(None), Cell(None), Cell(None)),
        Vector(Cell(None), Cell(None), Cell(None))
      )
    )

    assert(board.currentTurnWon)
  }

  test("Check winning condition for a column") {
    val board = Board(
      Vector(
        Vector(Cell(Some(Mark.X)), Cell(None), Cell(None)),
        Vector(Cell(Some(Mark.X)), Cell(None), Cell(None)),
        Vector(Cell(Some(Mark.X)), Cell(None), Cell(None))
      )
    )

    assert(board.currentTurnWon)
  }

  test("Check winning condition for main diagonal") {
    val board = Board(
      Vector(
        Vector(Cell(Some(Mark.X)), Cell(None), Cell(None)),
        Vector(Cell(None), Cell(Some(Mark.X)), Cell(None)),
        Vector(Cell(None), Cell(None), Cell(Some(Mark.X)))
      )
    )

    assert(board.currentTurnWon)
  }

  test("Check winning condition for anti-diagonal") {
    val board = Board(
      Vector(
        Vector(Cell(None), Cell(None), Cell(Some(Mark.X))),
        Vector(Cell(None), Cell(Some(Mark.X)), Cell(None)),
        Vector(Cell(Some(Mark.X)), Cell(None), Cell(None))
      )
    )

    assert(board.currentTurnWon)
  }

  test("Check no winning condition") {
    val board = Board(
      Vector(
        Vector(Cell(Some(Mark.X)), Cell(Some(Mark.O)), Cell(Some(Mark.X))),
        Vector(Cell(Some(Mark.O)), Cell(Some(Mark.X)), Cell(Some(Mark.O))),
        Vector(Cell(Some(Mark.O)), Cell(Some(Mark.X)), Cell(Some(Mark.O)))
      )
    )

    assert(!board.currentTurnWon)
  }
}
